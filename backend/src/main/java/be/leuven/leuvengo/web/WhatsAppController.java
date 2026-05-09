package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.PendingReport;
import be.leuven.leuvengo.domain.TrashPhoto;
import be.leuven.leuvengo.repository.PendingReportRepository;
import be.leuven.leuvengo.repository.TrashPhotoRepository;
import be.leuven.leuvengo.service.BlobStorageService;
import be.leuven.leuvengo.service.GeminiService;
import be.leuven.leuvengo.service.ReportService;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Twilio WhatsApp webhook — two-step flow:
 *   1. User shares a location pin  → opens a PendingReport and asks for a photo.
 *   2. User sends a photo          → scores via Gemini 1.5 Flash, saves to feed,
 *      pushes into the hotspot pipeline, and replies with the AI result.
 */
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppController.class);

    private final PendingReportRepository pending;
    private final TrashPhotoRepository photos;
    private final ReportService reportService;
    private final GeminiService gemini;
    private final BlobStorageService blobStorage;

    public WhatsAppController(PendingReportRepository pending,
                              TrashPhotoRepository photos,
                              ReportService reportService,
                              GeminiService gemini,
                              BlobStorageService blobStorage) {
        this.pending = pending;
        this.photos = photos;
        this.reportService = reportService;
        this.gemini = gemini;
        this.blobStorage = blobStorage;
    }

    @PostMapping(value = "/webhook", produces = MediaType.APPLICATION_XML_VALUE)
    public String webhook(
            @RequestParam(name = "From",      required = false) String from,
            @RequestParam(name = "Body",      required = false) String body,
            @RequestParam(name = "Latitude",  required = false) Double lat,
            @RequestParam(name = "Longitude", required = false) Double lng,
            @RequestParam(name = "MediaUrl0", required = false) String mediaUrl) {

        log.info("WhatsApp webhook from={} body='{}' lat={} lng={} media={}",
                from, body, lat, lng, mediaUrl);

        if (lat != null && lng != null) {
            return handleLocation(from, lat, lng);
        }
        if (mediaUrl != null && !mediaUrl.isBlank()) {
            return handlePhoto(from, mediaUrl);
        }
        return reply("Hi! Send a 📍 location pin to start a cleanliness report.");
    }

    private String handleLocation(String from, double lat, double lng) {
        PendingReport pr = new PendingReport();
        pr.setFromNumber(from);
        pr.setLat(lat);
        pr.setLng(lng);
        pr.setCreatedAt(Instant.now());
        pr.setCompleted(false);
        pending.save(pr);

        return reply("Got it! 📍 Now please send a photo of the trash "
                + "so our AI can assess the severity.");
    }

    private String handlePhoto(String from, String mediaUrl) {
        Optional<PendingReport> latest =
                from == null
                        ? pending.findFirstByCompletedFalseOrderByCreatedAtDesc()
                        : pending.findFirstByFromNumberAndCompletedFalseOrderByCreatedAtDesc(from)
                                .or(pending::findFirstByCompletedFalseOrderByCreatedAtDesc);

        if (latest.isEmpty()) {
            return reply("We don't have a location yet — please share a 📍 location pin first, "
                    + "then re-send the photo.");
        }

        PendingReport pr = latest.get();

        // AI cleanliness scoring + fraud detection via Gemini
        GeminiService.ScoreResult ai = gemini.scorePhoto(mediaUrl);

        if (ai.fake()) {
            log.warn("Fake photo rejected from {}: {}", from, ai.fakeReason());
            String reason = ai.fakeReason() != null ? ai.fakeReason() : "the image does not appear to be a real photo taken on location";
            return reply("🚫 Photo rejected\n\n"
                    + "Our AI detected that this doesn't look like a genuine on-site photo: "
                    + reason + ".\n\n"
                    + "Please take a real photo of the litter where you are standing and send it again.");
        }

        int cleanlinessScore = ai.score();          // 0-100 (100 = clean)
        int rating = toRating(cleanlinessScore);    // 1-5 dirtiness for pipeline

        // Upload to Azure Blob Storage; fall back to Twilio URL if upload fails
        String pseudoId = pseudoIdFor(from);
        String photoUrl = blobStorage.uploadFromTwilio(mediaUrl, "image/jpeg", pseudoId)
                .orElse(mediaUrl);

        pr.setMediaUrl(photoUrl);
        pr.setCleanlinessScore(rating);
        pr.setCompleted(true);
        pending.save(pr);

        // Push into hotspot pipeline (maps, work orders, leaderboard)
        try {
            reportService.submit(new ReportService.Submission(
                    pr.getLat(), pr.getLng(), rating,
                    "WhatsApp report",
                    photoUrl,
                    ai.tags().isEmpty() ? null : String.join(",", ai.tags()),
                    null,
                    pseudoId
            ));
        } catch (Exception ex) {
            log.warn("Failed to ingest WhatsApp report into hotspot pipeline", ex);
        }

        // Save to photo feed
        try {
            TrashPhoto photo = new TrashPhoto();
            photo.setUsername("WhatsApp user");
            photo.setPhotoUrl(photoUrl);
            photo.setLat(pr.getLat());
            photo.setLng(pr.getLng());
            photo.setCleanlinessScore(cleanlinessScore);
            photo.setUserRating(rating);
            photo.setTags(ai.tags().isEmpty() ? null : String.join(",", ai.tags()));
            photo.setReportedAt(Instant.now());
            photos.save(photo);
        } catch (Exception ex) {
            log.warn("Failed to save photo to feed", ex);
        }

        return reply(buildReply(cleanlinessScore, ai.description(), ai.tags()));
    }

    /** Converts 0-100 cleanliness score to a 1-5 dirtiness rating for the pipeline. */
    private static int toRating(int cleanlinessScore) {
        if (cleanlinessScore >= 80) return 1;
        if (cleanlinessScore >= 60) return 2;
        if (cleanlinessScore >= 40) return 3;
        if (cleanlinessScore >= 20) return 4;
        return 5;
    }

    private static String buildReply(int score, String description, List<String> tags) {
        String label = score >= 80 ? "Clean 🟢"
                : score >= 60 ? "Mild litter 🟡"
                : score >= 40 ? "Moderate waste 🟠"
                : "Critical — urgent cleanup needed 🔴";

        StringBuilder sb = new StringBuilder();
        sb.append("🤖 AI Analysis complete!\n\n");
        sb.append("Cleanliness score: ").append(score).append("/100 — ").append(label).append('\n');
        if (description != null && !description.isBlank()
                && !"AI scoring unavailable".equals(description)
                && !"scoring error".equals(description)) {
            sb.append("Assessment: ").append(description).append('\n');
        }
        if (!tags.isEmpty()) {
            sb.append("Spotted: ").append(
                    tags.stream().map(t -> "#" + t).collect(Collectors.joining(" "))
            ).append('\n');
        }
        sb.append("\nYour report is live on the Leuven Go feed! "
                + "Our crew has been notified. Thank you for keeping Leuven clean! 🌿");
        return sb.toString();
    }

    /** GDPR-friendly: hash the raw WhatsApp number so we never persist PII. */
    private static String pseudoIdFor(String from) {
        if (from == null) return "wa-anon";
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(from.getBytes());
            return "wa-" + HexFormat.of().formatHex(hash).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            return "wa-anon";
        }
    }

    private String reply(String text) {
        return new MessagingResponse.Builder()
                .message(new Message.Builder(text).build())
                .build()
                .toXml();
    }
}
