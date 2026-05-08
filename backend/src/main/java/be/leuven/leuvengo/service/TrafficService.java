package be.leuven.leuvengo.service;

import be.leuven.leuvengo.config.AppProperties;
import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.domain.StreetSegment;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.repository.StreetSegmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Traffic Logic: collapse multiple reports in the same vicinity into a single Hotspot
 * to prevent duplicate Planon work orders. Pure proximity clustering against open
 * hotspots, weighted by recency.
 */
@Service
public class TrafficService {

    private final HotspotRepository hotspots;
    private final StreetSegmentRepository segments;
    private final AppProperties props;

    public TrafficService(HotspotRepository hotspots,
                          StreetSegmentRepository segments,
                          AppProperties props) {
        this.hotspots = hotspots;
        this.segments = segments;
        this.props = props;
    }

    /**
     * Attach the given report to the best matching open hotspot, or create a new one.
     * Returns the hotspot the report was merged into.
     */
    @Transactional
    public Hotspot ingest(Report report) {
        List<Hotspot> open = hotspots.findAllByStatusIn(
                List.of(Hotspot.Status.OPEN, Hotspot.Status.ESCALATED));

        Hotspot best = null;
        double bestDist = Double.MAX_VALUE;
        for (Hotspot h : open) {
            double d = GeoUtil.distanceMeters(
                    report.getLat(), report.getLng(), h.getCenterLat(), h.getCenterLng());
            if (d <= props.getTraffic().getClusterRadiusM() && d < bestDist) {
                best = h;
                bestDist = d;
            }
        }

        if (best == null) {
            best = newHotspotFor(report);
        } else {
            mergeInto(best, report);
        }

        report.setHotspot(best);
        return hotspots.save(best);
    }

    private Hotspot newHotspotFor(Report report) {
        Hotspot h = new Hotspot();
        h.setCenterLat(report.getLat());
        h.setCenterLng(report.getLng());
        h.setSeverity(report.getCleanlinessRating().doubleValue());
        h.setReportCount(1);
        h.setStatus(Hotspot.Status.OPEN);
        h.setCreatedAt(Instant.now());
        h.setLastUpdatedAt(Instant.now());
        h.setLabel(deriveLabel(report.getLat(), report.getLng()));
        h.setStreetSegment(nearestSegment(report.getLat(), report.getLng()).orElse(null));
        return h;
    }

    private void mergeInto(Hotspot h, Report r) {
        int prev = h.getReportCount() == null ? 0 : h.getReportCount();
        int next = prev + 1;
        // weighted centroid
        double lat = (h.getCenterLat() * prev + r.getLat()) / next;
        double lng = (h.getCenterLng() * prev + r.getLng()) / next;
        // running mean severity
        double sev = ((h.getSeverity() == null ? 0 : h.getSeverity()) * prev
                + r.getCleanlinessRating()) / next;
        h.setCenterLat(lat);
        h.setCenterLng(lng);
        h.setReportCount(next);
        h.setSeverity(sev);
        h.setLastUpdatedAt(Instant.now());
    }

    private Optional<StreetSegment> nearestSegment(double lat, double lng) {
        return segments.findAll().stream()
                .filter(s -> s.getCenterLat() != null && s.getCenterLng() != null)
                .min((a, b) -> Double.compare(
                        GeoUtil.distanceMeters(lat, lng, a.getCenterLat(), a.getCenterLng()),
                        GeoUtil.distanceMeters(lat, lng, b.getCenterLat(), b.getCenterLng())));
    }

    private String deriveLabel(double lat, double lng) {
        return nearestSegment(lat, lng).map(StreetSegment::getName).orElse("Leuven");
    }
}
