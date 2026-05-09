package be.leuven.leuvengo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=";

    private static final String PROMPT =
            "You are a fraud-detecting cleanliness inspector for a city reporting app. "
            + "First, decide if this photo is FAKE. Mark it fake if ANY of these apply: "
            + "(1) the image appears to be a photo of a screen, monitor, TV, or phone display "
            + "(look for screen glare, pixel grids, Moiré patterns, device bezels, or curved display edges); "
            + "(2) the image looks like a stock photo, watermarked image, or search-engine result; "
            + "(3) the image shows an indoor scene with no outdoor urban context; "
            + "(4) there is no real-world depth, lighting, or perspective — it looks flat or digitally generated. "
            + "If the photo is NOT fake, rate the cleanliness of the outdoor urban space on a scale of 0 to 100 "
            + "where 100 = perfectly clean and 0 = completely covered in hazardous waste. "
            + "Respond with ONLY valid JSON — no markdown fences, no prose: "
            + "{\"fake\": <true|false>, \"reason\": \"<one short sentence why it is fake, or null if genuine>\", "
            + "\"score\": <0-100 or null if fake>, \"description\": \"<one short sentence or null if fake>\", "
            + "\"tags\": [\"<tag1>\", \"<tag2>\"]}. "
            + "Tags must name visible waste types such as plastic, glass, paper, food_waste, cigarettes. "
            + "Include at most 3 tags. If fake, tags may be empty.";

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public record ScoreResult(boolean fake, String fakeReason, int score, String description, List<String> tags) {}

    /**
     * Downloads the image at {@code imageUrl}, sends it to Gemini 1.5 Flash,
     * and returns a cleanliness score (0-100), description, and waste tags.
     * Falls back gracefully when the API key is absent or the call fails.
     */
    public ScoreResult scorePhoto(String imageUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY not configured — returning fallback score");
            return new ScoreResult(false, null, 50, "AI scoring unavailable", List.of());
        }
        try {
            ImageData image = downloadImage(imageUrl);
            return callGemini(image);
        } catch (Exception e) {
            log.error("Gemini scoring failed for {}: {}", imageUrl, e.getMessage());
            return new ScoreResult(false, null, 50, "scoring error", List.of());
        }
    }

    private record ImageData(byte[] bytes, String mimeType) {}

    private ImageData downloadImage(String url) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
        // Twilio media URLs require Basic auth with account credentials
        if (url.contains("api.twilio.com") && !twilioAccountSid.isBlank()) {
            String creds = twilioAccountSid + ":" + twilioAuthToken;
            builder.header("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(creds.getBytes()));
        }
        HttpResponse<byte[]> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 300) {
            throw new RuntimeException("Image download failed: HTTP " + resp.statusCode());
        }
        String contentType = resp.headers().firstValue("Content-Type")
                .orElse("image/jpeg")
                .split(";")[0].trim();
        log.info("Downloaded image: {} bytes, type={}", resp.body().length, contentType);
        return new ImageData(resp.body(), contentType);
    }

    private ScoreResult callGemini(ImageData image) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(image.bytes());
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", PROMPT),
                                Map.of("inlineData", Map.of("mimeType", image.mimeType(), "data", base64))
                        )
                ))
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new RuntimeException("Gemini API error " + resp.statusCode() + ": " + resp.body());
        }

        JsonNode root = mapper.readTree(resp.body());
        String text = root.at("/candidates/0/content/parts/0/text").asText("{}").strip();

        // Strip markdown code fences if Gemini adds them despite the prompt
        if (text.startsWith("```")) {
            text = text.replaceAll("(?s)```[a-z]*\\n?", "").replace("```", "").strip();
        }

        JsonNode result = mapper.readTree(text);
        boolean fake = result.path("fake").asBoolean(false);
        String fakeReason = result.path("reason").isNull() ? null : result.path("reason").asText(null);
        int score = fake ? 0 : Math.max(0, Math.min(100, result.path("score").asInt(50)));
        String description = result.path("description").isNull() ? null : result.path("description").asText(null);

        List<String> tags = List.of();
        if (result.has("tags") && result.get("tags").isArray()) {
            tags = mapper.convertValue(
                    result.get("tags"),
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        }

        log.info("Gemini result: fake={} reason='{}' score={} description='{}' tags={}",
                fake, fakeReason, score, description, tags);
        return new ScoreResult(fake, fakeReason, score, description, tags);
    }
}
