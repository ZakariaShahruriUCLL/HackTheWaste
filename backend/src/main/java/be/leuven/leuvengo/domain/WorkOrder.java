package be.leuven.leuvengo.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "work_order")
public class WorkOrder {

    public enum Status { PENDING, DISPATCHED, IN_PROGRESS, COMPLETED, FAILED }
    public enum Priority { LOW, MEDIUM, HIGH, URGENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Reference id returned by Planon (or our mock). */
    private String planonRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotspot_id", nullable = false)
    private Hotspot hotspot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    private String crew;
    private String summary;

    private Instant createdAt;
    private Instant dispatchedAt;
    private Instant completedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanonRef() { return planonRef; }
    public void setPlanonRef(String planonRef) { this.planonRef = planonRef; }
    public Hotspot getHotspot() { return hotspot; }
    public void setHotspot(Hotspot hotspot) { this.hotspot = hotspot; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public String getCrew() { return crew; }
    public void setCrew(String crew) { this.crew = crew; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(Instant dispatchedAt) { this.dispatchedAt = dispatchedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
