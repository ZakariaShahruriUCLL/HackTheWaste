package be.leuven.leuvengo.service;

import be.leuven.leuvengo.config.AppProperties;
import be.leuven.leuvengo.domain.Faculty;
import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reports;
    private final FacultyRepository faculties;
    private final TrafficService traffic;
    private final ScoringService scoring;
    private final PlanonService planon;
    private final AppProperties props;

    public ReportService(ReportRepository reports,
                         FacultyRepository faculties,
                         TrafficService traffic,
                         ScoringService scoring,
                         PlanonService planon,
                         AppProperties props) {
        this.reports = reports;
        this.faculties = faculties;
        this.traffic = traffic;
        this.scoring = scoring;
        this.planon = planon;
        this.props = props;
    }

    public record Submission(Double lat, Double lng, Integer rating,
                             String note, String imageRef,
                             String signalTags, String facultyShortCode,
                             String reporterPseudoId) {}

    @Transactional
    public Report submit(Submission s) {
        validate(s);

        Report r = new Report();
        r.setLat(s.lat());
        r.setLng(s.lng());
        r.setCleanlinessRating(s.rating());
        r.setNote(safe(s.note(), 500));
        r.setImageRef(s.imageRef());
        r.setSignalTags(safe(s.signalTags(), 200));
        r.setReportedAt(Instant.now());
        r.setReporterPseudoId(s.reporterPseudoId() != null ? s.reporterPseudoId()
                : "anon-" + UUID.randomUUID().toString().substring(0, 8));

        if (s.facultyShortCode() != null) {
            faculties.findByShortCode(s.facultyShortCode())
                    .ifPresent(f -> {
                        r.setCrediteFaculty(f);
                        // award points - dirty reports earn more (unique signal value)
                        int award = 6 - s.rating();
                        f.setPoints((f.getPoints() == null ? 0 : f.getPoints()) + Math.max(1, award));
                        faculties.save(f);
                    });
        }

        Report saved = reports.save(r);
        Hotspot h = traffic.ingest(saved);

        // Auto-escalate to Planon when threshold crossed.
        int threshold = props.getTraffic().getWorkOrderThreshold();
        if (h.getStatus() == Hotspot.Status.OPEN
                && h.getReportCount() != null
                && h.getReportCount() >= threshold) {
            h.setStatus(Hotspot.Status.ESCALATED);
            planon.dispatch(h);
        }

        scoring.recomputeAll();
        return saved;
    }

    private void validate(Submission s) {
        if (s.lat() == null || s.lng() == null) {
            throw new IllegalArgumentException("Coordinates required");
        }
        if (s.rating() == null || s.rating() < 0 || s.rating() > 5) {
            throw new IllegalArgumentException("Rating must be 0-5");
        }
    }

    private String safe(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }

    public Faculty crediteFacultyOrDefault(String code) {
        return faculties.findByShortCode(code)
                .orElseGet(() -> faculties.findAll().stream().findFirst().orElse(null));
    }
}
