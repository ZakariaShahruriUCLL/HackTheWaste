package be.leuven.leuvengo.service;

import be.leuven.leuvengo.config.AppProperties;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.domain.StreetSegment;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.repository.StreetSegmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Computes a 0-100 "AI cleanliness score" per street segment.
 * - Base score 100
 * - Subtract weighted severity from recent reports within the segment radius
 * - Decay older reports with a recency factor
 *
 * Real production version would also fold in image-based litter detection,
 * camera coverage and seasonal student-density. We expose the same interface.
 */
@Service
public class ScoringService {

    private static final double SEGMENT_RADIUS_M = 80;

    private final ReportRepository reports;
    private final StreetSegmentRepository segments;
    private final AppProperties props;

    public ScoringService(ReportRepository reports,
                          StreetSegmentRepository segments,
                          AppProperties props) {
        this.reports = reports;
        this.segments = segments;
        this.props = props;
    }

    @Transactional
    public void recomputeAll() {
        Instant since = Instant.now().minus(props.getTraffic().getReportTtlHours(), ChronoUnit.HOURS);
        List<Report> recent = reports.findRecent(since);
        for (StreetSegment seg : segments.findAll()) {
            double score = scoreFor(seg, recent);
            seg.setAiCleanlinessScore(round1(score));
            seg.setReportCount30d((int) recent.stream()
                    .filter(r -> within(seg, r))
                    .count());
            seg.setLastEvaluatedAt(Instant.now());
            segments.save(seg);
        }
    }

    private double scoreFor(StreetSegment seg, List<Report> recent) {
        double penalty = 0d;
        for (Report r : recent) {
            if (!within(seg, r)) continue;
            // rating 0 -> filthy -> penalty 12. rating 5 -> pristine -> +1.
            double sevPenalty = (3.0 - r.getCleanlinessRating()) * 4;
            double recency = recencyFactor(r.getReportedAt());
            penalty += sevPenalty * recency;
        }
        double score = 100 - penalty;
        if (score < 0) score = 0;
        if (score > 100) score = 100;
        return score;
    }

    private boolean within(StreetSegment seg, Report r) {
        if (seg.getCenterLat() == null || seg.getCenterLng() == null) return false;
        return GeoUtil.distanceMeters(seg.getCenterLat(), seg.getCenterLng(),
                r.getLat(), r.getLng()) <= SEGMENT_RADIUS_M;
    }

    private double recencyFactor(Instant when) {
        if (when == null) return 0.5;
        long hours = Math.max(0, ChronoUnit.HOURS.between(when, Instant.now()));
        // exponential decay - 24h half-life
        return Math.pow(0.5, hours / 24.0);
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
