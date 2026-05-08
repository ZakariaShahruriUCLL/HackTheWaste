package be.leuven.leuvengo.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "street_segment")
public class StreetSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String district;
    private Double centerLat;
    private Double centerLng;
    /** 0-100 - higher = cleaner. Derived from rolling reports + image signals. */
    private Double aiCleanlinessScore;
    private Integer reportCount30d;
    private Instant lastEvaluatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public Double getCenterLat() { return centerLat; }
    public void setCenterLat(Double centerLat) { this.centerLat = centerLat; }
    public Double getCenterLng() { return centerLng; }
    public void setCenterLng(Double centerLng) { this.centerLng = centerLng; }
    public Double getAiCleanlinessScore() { return aiCleanlinessScore; }
    public void setAiCleanlinessScore(Double v) { this.aiCleanlinessScore = v; }
    public Integer getReportCount30d() { return reportCount30d; }
    public void setReportCount30d(Integer v) { this.reportCount30d = v; }
    public Instant getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(Instant v) { this.lastEvaluatedAt = v; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
}
