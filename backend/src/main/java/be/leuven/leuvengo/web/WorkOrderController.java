package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.WorkOrder;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import be.leuven.leuvengo.service.AuthService;
import be.leuven.leuvengo.service.PlanonService;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderRepository workOrders;
    private final HotspotRepository hotspots;
    private final PlanonService planon;
    private final AuthService authService;

    public WorkOrderController(WorkOrderRepository workOrders,
                               HotspotRepository hotspots,
                               PlanonService planon,
                               AuthService authService) {
        this.workOrders = workOrders;
        this.hotspots = hotspots;
        this.planon = planon;
        this.authService = authService;
    }

    @GetMapping
    public List<Dtos.WorkOrderDto> list() {
        return workOrders.findTop20ByOrderByCreatedAtDesc().stream()
                .map(Dtos::of).toList();
    }

    @PostMapping("/{id}/complete")
    @Transactional
    public ResponseEntity<Dtos.WorkOrderDto> complete(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        authService.requireAuth(auth);
        WorkOrder w = workOrders.findById(id).orElseThrow();
        w.setStatus(WorkOrder.Status.COMPLETED);
        w.setCompletedAt(Instant.now());
        if (w.getHotspot() != null) {
            Hotspot h = w.getHotspot();
            h.setStatus(Hotspot.Status.RESOLVED);
            h.setLastUpdatedAt(Instant.now());
            hotspots.save(h);
        }
        return ResponseEntity.ok(Dtos.of(workOrders.save(w)));
    }

    @PostMapping("/dispatch/{hotspotId}")
    public ResponseEntity<Dtos.WorkOrderDto> manualDispatch(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long hotspotId) {
        authService.requireAuth(auth);
        Hotspot h = hotspots.findById(hotspotId).orElseThrow();
        return ResponseEntity.ok(Dtos.of(planon.dispatch(h)));
    }
}
