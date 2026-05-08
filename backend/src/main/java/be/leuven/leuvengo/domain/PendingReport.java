package be.leuven.leuvengo.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Two-step WhatsApp report state. A user first shares a location pin
 * (lat/lng captured), then a photo (mediaUrl + cleanlinessScore captured).
 * Once both are present we mark it completed.
 */
@Entity
@Table(name = "pending_report", indexes = {
        @Index(name = "idx_pending_from", columnList = "fromNumber,completed,createdAt")
})
public class PendingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Twilio sender, e.g. "whatsapp:+32477123456". */
    @Column(length = 64)
    private String fromNumber;

    private Double lat;
    private Double lng;

    @Column(length = 1024)
    private String mediaUrl;

    /** Mock 0-5 cleanliness score assigned once a photo arrives. */
    private Integer cleanlinessScore;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Boolean completed = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFromNumber() { return fromNumber; }
    public void setFromNumber(String v) { this.fromNumber = v; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String v) { this.mediaUrl = v; }
    public Integer getCleanlinessScore() { return cleanlinessScore; }
    public void setCleanlinessScore(Integer v) { this.cleanlinessScore = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean v) { this.completed = v; }
}
