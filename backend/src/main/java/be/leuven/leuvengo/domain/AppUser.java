package be.leuven.leuvengo.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 72)
    private String passwordHash;

    /** Optional — chosen at signup, used later to auto-credit WhatsApp reports. */
    @Column(length = 10)
    private String facultyShortCode;

    /** Rotated on every login, cleared on logout. Acts as a simple bearer token. */
    @Column(length = 64, unique = true)
    private String sessionToken;

    /** Whether the user gave explicit data-use consent at signup. */
    @Column(nullable = false)
    private boolean consentGiven = false;

    /** When consent was recorded — kept for audit / GDPR accountability. */
    @Column
    private Instant consentGivenAt;

    @Column(nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFacultyShortCode() { return facultyShortCode; }
    public void setFacultyShortCode(String facultyShortCode) { this.facultyShortCode = facultyShortCode; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public boolean isConsentGiven() { return consentGiven; }
    public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }
    public Instant getConsentGivenAt() { return consentGivenAt; }
    public void setConsentGivenAt(Instant consentGivenAt) { this.consentGivenAt = consentGivenAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
