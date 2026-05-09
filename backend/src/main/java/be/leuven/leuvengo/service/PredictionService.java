package be.leuven.leuvengo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Calls the Python FastAPI ML service to get predicted trash likelihood scores.
 * Falls back gracefully to 0.0 if the service is unavailable.
 */
@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Value("${ml.api-url:http://localhost:8000}")
    private String mlApiUrl;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PredictPoint(double lat, double lng, double likelihood) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GridResponse(String time, int count, List<PredictPoint> points) {}

    /**
     * Returns a grid of predicted trash likelihood across Leuven at the given time.
     * The result is ready to be pushed to the frontend as a heatmap data source.
     */
    public List<PredictPoint> predictGrid(LocalDateTime at, int steps) {
        try {
            var body = mapper.writeValueAsString(Map.of(
                    "time",      at.format(ISO),
                    "lat_min",   50.855,
                    "lat_max",   50.905,
                    "lng_min",   4.670,
                    "lng_max",   4.730,
                    "lat_steps", steps,
                    "lng_steps", steps
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(mlApiUrl + "/predict/grid"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("ML API returned HTTP {}", resp.statusCode());
                return List.of();
            }
            GridResponse grid = mapper.readValue(resp.body(), GridResponse.class);
            return grid.points();
        } catch (Exception e) {
            log.warn("ML API unavailable — predictive heatmap skipped: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Predicts trash likelihood for a single point (used by the report flow).
     */
    public double predictPoint(double lat, double lng, LocalDateTime at) {
        try {
            var body = mapper.writeValueAsString(Map.of(
                    "latitude",  lat,
                    "longitude", lng,
                    "time",      at.format(ISO)
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(mlApiUrl + "/predict"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return 0.0;
            var node = mapper.readTree(resp.body());
            return node.path("likelihood").asDouble(0.0);
        } catch (Exception e) {
            log.warn("ML point prediction failed: {}", e.getMessage());
            return 0.0;
        }
    }
}
