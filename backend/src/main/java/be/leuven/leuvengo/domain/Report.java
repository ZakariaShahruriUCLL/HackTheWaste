package be.leuven.leuvengo.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A single citizen / student report. GDPR-friendly:
 * - no personal identifiers are persisted (only an anonymous pseudo-id)
 * - image is referenced by hash/url, not stored as raw bytes here
 * - only derived metrics are kept long term
 */
@Entity
@Table(name = "report", indexes = {
        @Index(name = "idx_report_geo", columnList = "lat,lng"),
        @Index(name = "idx_report_hotspot", columnList = "hotspot_id")
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    /** 0 = filthy, 5 = pristine. */
    @Column(nullable = false)
    private Integer cleanlinessRating;

    /** Free-text observation, e.g. "overflowing bin", "broken glass". */
    @Column(length = 500)
    private String note;

    /** URL or hash pointer for the photo. */
    private String imageRef;

    /** Tags inferred from image / note (litter, graffiti, overflow...). */
    @Column(length = 200)
    private String signalTags;

    /** Anonymous reporter id - rotates per session, never PII. */
    private String reporterPseudoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty crediteFaculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotspot_id")
    private Hotspot hotspot;

    @Column(nullable = false)
    private Instant reportedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public Integer getCleanlinessRating() { return cleanlinessRating; }
    public void setCleanlinessRating(Integer v) { this.cleanlinessRating = v; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getImageRef() { return imageRef; }
    public void setImageRef(String imageRef) { this.imageRef = imageRef; }
    public String getSignalTags() { return signalTags; }
    public void setSignalTags(String s) { this.signalTags = s; }
    public String getReporterPseudoId() { return reporterPseudoId; }
    public void setReporterPseudoId(String v) { this.reporterPseudoId = v; }
    public Faculty getCrediteFaculty() { return crediteFaculty; }
    public void setCrediteFaculty(Faculty f) { this.crediteFaculty = f; }
    public Hotspot getHotspot() { return hotspot; }
    public void setHotspot(Hotspot hotspot) { this.hotspot = hotspot; }
    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
}
