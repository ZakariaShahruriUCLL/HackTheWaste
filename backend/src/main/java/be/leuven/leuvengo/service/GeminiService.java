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
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private static final String PROMPT =
            "Analyze this image of waste/trash in a public urban space in Leuven, Belgium. "
            + "Rate the cleanliness on a scale of 0 to 100 where 100 = perfectly clean (no litter at all) "
            + "and 0 = completely covered in hazardous or overwhelming waste. "
            + "Respond with ONLY valid JSON — no markdown fences, no prose: "
            + "{\"score\": <0-100>, \"description\": \"<one short sentence>\", "
            + "\"tags\": [\"<tag1>\", \"<tag2>\"]}. "
            + "Tags must name visible waste types such as plastic, glass, paper, food_waste, cigarettes. "
            + "Include at most 3 tags.";

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public record ScoreResult(int score, String description, List<String> tags) {}

    /**
     * Downloads the image at {@code imageUrl}, sends it to Gemini 1.5 Flash,
     * and returns a cleanliness score (0-100), description, and waste tags.
     * Falls back gracefully when the API key is absent or the call fails.
     */
    public ScoreResult scorePhoto(String imageUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY not configured — returning fallback score");
            return new ScoreResult(50, "AI scoring unavailable", List.of());
        }
        try {
            byte[] bytes = downloadImage(imageUrl);
            return callGemini(bytes);
        } catch (Exception e) {
            log.error("Gemini scoring failed for {}: {}", imageUrl, e.getMessage());
            return new ScoreResult(50, "scoring error", List.of());
        }
    }

    private byte[] downloadImage(String url) throws Exception {
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
        return resp.body();
    }

    private ScoreResult callGemini(byte[] imageBytes) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", PROMPT),
                                Map.of("inlineData", Map.of("mimeType", "image/jpeg", "data", base64))
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
        int score = Math.max(0, Math.min(100, result.path("score").asInt(50)));
        String description = result.path("description").asText("no description");

        List<String> tags = List.of();
        if (result.has("tags") && result.get("tags").isArray()) {
            tags = mapper.convertValue(
                    result.get("tags"),
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        }

        log.info("Gemini result: score={} description='{}' tags={}", score, description, tags);
        return new ScoreResult(score, description, tags);
    }
}
