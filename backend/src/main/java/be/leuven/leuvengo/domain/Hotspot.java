package be.leuven.leuvengo.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "hotspot")
public class Hotspot {

    public enum Status { OPEN, ESCALATED, DISPATCHED, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double centerLat;

    @Column(nullable = false)
    private Double centerLng;

    /** Mean rating across merged reports (0-5, lower is dirtier). */
    private Double severity;

    /** How many reports merged into this hotspot. */
    private Integer reportCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "street_segment_id")
    private StreetSegment streetSegment;

    private Instant createdAt;
    private Instant lastUpdatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Double getCenterLat() { return centerLat; }
    public void setCenterLat(Double centerLat) { this.centerLat = centerLat; }
    public Double getCenterLng() { return centerLng; }
    public void setCenterLng(Double centerLng) { this.centerLng = centerLng; }
    public Double getSeverity() { return severity; }
    public void setSeverity(Double severity) { this.severity = severity; }
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public StreetSegment getStreetSegment() { return streetSegment; }
    public void setStreetSegment(StreetSegment s) { this.streetSegment = s; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
