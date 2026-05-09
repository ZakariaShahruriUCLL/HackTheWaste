package be.leuven.leuvengo.web;

import be.leuven.leuvengo.service.PredictionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService prediction;

    public PredictionController(PredictionService prediction) {
        this.prediction = prediction;
    }

    /**
     * GET /api/predictions/grid?at=2026-05-09T22:00:00&steps=30
     *
     * Returns a grid of predicted trash likelihood points for the heatmap.
     * `at` defaults to now if omitted; `steps` controls grid density (default 30).
     */
    @GetMapping("/grid")
    public List<PredictionService.PredictPoint> grid(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime at,
            @RequestParam(defaultValue = "30") int steps) {
        LocalDateTime time = at != null ? at : LocalDateTime.now();
        int clampedSteps = Math.min(Math.max(steps, 5), 60);
        return prediction.predictGrid(time, clampedSteps);
    }

    /**
     * GET /api/predictions/point?lat=50.879&lng=4.700&at=2026-05-09T22:00:00
     */
    @GetMapping("/point")
    public double point(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime at) {
        LocalDateTime time = at != null ? at : LocalDateTime.now();
        return prediction.predictPoint(lat, lng, time);
    }
}
