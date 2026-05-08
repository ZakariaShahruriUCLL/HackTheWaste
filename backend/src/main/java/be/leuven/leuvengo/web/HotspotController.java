package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hotspots")
public class HotspotController {

    private final HotspotRepository hotspots;

    public HotspotController(HotspotRepository hotspots) {
        this.hotspots = hotspots;
    }

    @GetMapping
    public List<Dtos.HotspotDto> list() {
        return hotspots.findAll().stream()
                .sorted((a, b) -> Integer.compare(
                        b.getReportCount() == null ? 0 : b.getReportCount(),
                        a.getReportCount() == null ? 0 : a.getReportCount()))
                .map(Dtos::of)
                .toList();
    }

    @GetMapping("/active")
    public List<Dtos.HotspotDto> active() {
        return hotspots.findAllByStatusIn(List.of(
                Hotspot.Status.OPEN, Hotspot.Status.ESCALATED, Hotspot.Status.DISPATCHED))
                .stream().map(Dtos::of).toList();
    }
}
