package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.PendingReport;
import be.leuven.leuvengo.repository.PendingReportRepository;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

/**
 * Twilio WhatsApp webhook. Two-step flow:
 *   1. User shares a location pin  → we open a {@link PendingReport}
 *      and ask them for a photo.
 *   2. User sends a photo          → we attach mediaUrl + a mock score
 *      to the most recent open PendingReport (preferring same sender)
 *      and confirm receipt.
 *
 * Reply bodies are built with the Twilio SDK's {@link MessagingResponse}
 * so the TwiML XML is always well-formed.
 */
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppController.class);

    /** Mock cleanliness score until a real classifier is wired in. */
    private static final int MOCK_SCORE = 3;

    private final PendingReportRepository pending;

    public WhatsAppController(PendingReportRepository pending) {
        this.pending = pending;
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

        return reply("Thank you! Please send a picture of the trash to help us "
                + "assess the emergency (0-5).");
    }

    private String handlePhoto(String from, String mediaUrl) {
        Optional<PendingReport> latest =
                from == null
                        ? pending.findFirstByCompletedFalseOrderByCreatedAtDesc()
                        : pending.findFirstByFromNumberAndCompletedFalseOrderByCreatedAtDesc(from)
                                .or(pending::findFirstByCompletedFalseOrderByCreatedAtDesc);

        if (latest.isEmpty()) {
            return reply("We don't have a location yet — please share a 📍 location "
                    + "pin first, then re-send the photo.");
        }

        PendingReport pr = latest.get();
        pr.setMediaUrl(mediaUrl);
        pr.setCleanlinessScore(MOCK_SCORE);
        pr.setCompleted(true);
        pending.save(pr);

        return reply("Report received! Cleanliness level: " + MOCK_SCORE
                + ". Our crews are on it!");
    }

    private String reply(String text) {
        return new MessagingResponse.Builder()
                .message(new Message.Builder(text).build())
                .build()
                .toXml();
    }
}