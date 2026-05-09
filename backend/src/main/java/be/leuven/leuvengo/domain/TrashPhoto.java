package be.leuven.leuvengo.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One entry in the public photo feed.
 * Populated by the WhatsApp controller after uploading to Azure Blob Storage,
 * or by the web report flow if a photo was attached.
 *
 * cleanlinessScore is left null until the AI classifier runs (future work).
 */
@Entity
@Table(name = "trash_photo", indexes = {
        @Index(name = "idx_trash_photo_reported", columnList = "reportedAt")
})
public class TrashPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name shown in the feed (email prefix or "WhatsApp user"). */
    @Column(length = 100)
    private String username;

    /** Nullable — only present for registered web users. Never shown publicly. */
    @Column(length = 255)
    private String email;

    @Column(length = 10)
    private String facultyShortCode;

    @Column(length = 10)
    private String facultyEmoji;

    @Column(length = 20)
    private String facultyColor;

    /** Azure Blob Storage URL. Null until the photo has been uploaded. */
    @Column(length = 1024)
    private String photoUrl;

    private Double lat;
    private Double lng;

    @Column(length = 100)
    private String segmentName;

    /** 0–100 AI cleanliness score. Null until classifier runs. */
    private Integer cleanlinessScore;

    /** 0–5 rating submitted by the reporter. */
    private Integer userRating;

    @Column(length = 300)
    private String tags;

    @Column(nullable = false)
    private Instant reportedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFacultyShortCode() { return facultyShortCode; }
    public void setFacultyShortCode(String facultyShortCode) { this.facultyShortCode = facultyShortCode; }
    public String getFacultyEmoji() { return facultyEmoji; }
    public void setFacultyEmoji(String facultyEmoji) { this.facultyEmoji = facultyEmoji; }
    public String getFacultyColor() { return facultyColor; }
    public void setFacultyColor(String facultyColor) { this.facultyColor = facultyColor; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public String getSegmentName() { return segmentName; }
    public void setSegmentName(String segmentName) { this.segmentName = segmentName; }
    public Integer getCleanlinessScore() { return cleanlinessScore; }
    public void setCleanlinessScore(Integer cleanlinessScore) { this.cleanlinessScore = cleanlinessScore; }
    public Integer getUserRating() { return userRating; }
    public void setUserRating(Integer userRating) { this.userRating = userRating; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
}
