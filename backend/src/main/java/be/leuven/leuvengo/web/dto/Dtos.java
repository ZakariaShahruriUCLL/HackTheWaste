package be.leuven.leuvengo.web.dto;

import be.leuven.leuvengo.domain.*;
import be.leuven.leuvengo.domain.TrashPhoto;

import java.time.Instant;
import java.util.List;

/** Lightweight, JSON-friendly snapshots of domain entities. */
public final class Dtos {

    private Dtos() {}

    public record ReportDto(Long id, Double lat, Double lng, Integer rating,
                            String note, String imageRef, String signalTags,
                            String faculty, String facultyColor,
                            Long hotspotId, Instant reportedAt) {}

    public record HotspotDto(Long id, Double lat, Double lng, Double severity,
                             Integer reportCount, String status, String label,
                             String segmentName, Instant createdAt, Instant lastUpdatedAt) {}

    public record StreetSegmentDto(Long id, String name, String district,
                                   Double lat, Double lng, Double aiCleanlinessScore,
                                   Integer reportCount30d, String facultyShortCode,
                                   String facultyName, String facultyColor,
                                   Instant lastEvaluatedAt) {}

    public record FacultyDto(Long id, String name, String shortCode, String color,
                             String emoji, Integer members, Integer points,
                             String territoryGeoJson) {}

    public record LeaderboardEntry(Long id, String name, String shortCode,
                                   String color, String emoji, Integer points,
                                   Integer rank) {}

    public record WorkOrderDto(Long id, String planonRef, Long hotspotId,
                               String status, String priority, String crew,
                               String summary, Double lat, Double lng,
                               Instant createdAt, Instant dispatchedAt,
                               Instant completedAt) {}

    public record RewardItemDto(Long id, String title, String description, String sponsor,
                                String imageRef, String category, Integer costPoints,
                                Integer stock) {}

    public record StatsDto(long totalReports, long openHotspots, long dispatchedOrders,
                           long completedOrders, double avgCleanliness,
                           List<TimeBucket> reportsLast7Days,
                           List<HotspotDto> topHotspots) {}

    public record TimeBucket(String label, long count) {}

    public record TrashPhotoDto(
            Long id, String username, String email,
            String facultyShortCode, String facultyEmoji, String facultyColor,
            String photoUrl, Double lat, Double lng, String segmentName,
            Integer cleanlinessScore, Integer userRating, String tags,
            Instant reportedAt) {}

    public static ReportDto of(Report r) {
        return new ReportDto(
                r.getId(), r.getLat(), r.getLng(), r.getCleanlinessRating(),
                r.getNote(), r.getImageRef(), r.getSignalTags(),
                r.getCrediteFaculty() != null ? r.getCrediteFaculty().getName() : null,
                r.getCrediteFaculty() != null ? r.getCrediteFaculty().getColor() : null,
                r.getHotspot() != null ? r.getHotspot().getId() : null,
                r.getReportedAt());
    }

    public static HotspotDto of(Hotspot h) {
        return new HotspotDto(
                h.getId(), h.getCenterLat(), h.getCenterLng(), h.getSeverity(),
                h.getReportCount(),
                h.getStatus() != null ? h.getStatus().name() : null,
                h.getLabel(),
                h.getStreetSegment() != null ? h.getStreetSegment().getName() : null,
                h.getCreatedAt(), h.getLastUpdatedAt());
    }

    public static StreetSegmentDto of(StreetSegment s) {
        Faculty f = s.getFaculty();
        return new StreetSegmentDto(
                s.getId(), s.getName(), s.getDistrict(), s.getCenterLat(), s.getCenterLng(),
                s.getAiCleanlinessScore(), s.getReportCount30d(),
                f != null ? f.getShortCode() : null,
                f != null ? f.getName() : null,
                f != null ? f.getColor() : null,
                s.getLastEvaluatedAt());
    }

    public static FacultyDto of(Faculty f) {
        return new FacultyDto(f.getId(), f.getName(), f.getShortCode(), f.getColor(),
                f.getEmoji(), f.getMembers(), f.getPoints(), f.getTerritoryGeoJson());
    }

    public static WorkOrderDto of(WorkOrder w) {
        Hotspot h = w.getHotspot();
        return new WorkOrderDto(
                w.getId(), w.getPlanonRef(),
                h != null ? h.getId() : null,
                w.getStatus() != null ? w.getStatus().name() : null,
                w.getPriority() != null ? w.getPriority().name() : null,
                w.getCrew(), w.getSummary(),
                h != null ? h.getCenterLat() : null,
                h != null ? h.getCenterLng() : null,
                w.getCreatedAt(), w.getDispatchedAt(), w.getCompletedAt());
    }

    public static RewardItemDto of(RewardItem r) {
        return new RewardItemDto(r.getId(), r.getTitle(), r.getDescription(),
                r.getSponsor(), r.getImageRef(), r.getCategory(),
                r.getCostPoints(), r.getStock());
    }

    public static TrashPhotoDto of(TrashPhoto p) {
        return new TrashPhotoDto(
                p.getId(), p.getUsername(), p.getEmail(),
                p.getFacultyShortCode(), p.getFacultyEmoji(), p.getFacultyColor(),
                p.getPhotoUrl(), p.getLat(), p.getLng(), p.getSegmentName(),
                p.getCleanlinessScore(), p.getUserRating(), p.getTags(),
                p.getReportedAt());
    }
}
