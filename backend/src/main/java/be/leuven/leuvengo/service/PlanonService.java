package be.leuven.leuvengo.service;

import be.leuven.leuvengo.config.AppProperties;
import be.leuven.leuvengo.domain.Hotspot;
import be.leuven.leuvengo.domain.WorkOrder;
import be.leuven.leuvengo.repository.HotspotRepository;
import be.leuven.leuvengo.repository.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Planon integration. In production this would POST to Planon's
 * /api/services/{ws}/Order/Create or equivalent endpoint and persist the
 * returned reference. Here we mock the call but keep the shape so swapping in
 * a real {@link org.springframework.web.client.RestClient} is a small change.
 */
@Service
public class PlanonService {

    private static final Logger log = LoggerFactory.getLogger(PlanonService.class);

    private final WorkOrderRepository workOrders;
    private final HotspotRepository hotspots;
    private final AppProperties props;

    public PlanonService(WorkOrderRepository workOrders,
                         HotspotRepository hotspots,
                         AppProperties props) {
        this.workOrders = workOrders;
        this.hotspots = hotspots;
        this.props = props;
    }

    /** Idempotent: if the hotspot is already escalated/dispatched, return existing order. */
    @Transactional
    public WorkOrder dispatch(Hotspot hotspot) {
        if (hotspot.getStatus() == Hotspot.Status.DISPATCHED ||
                hotspot.getStatus() == Hotspot.Status.RESOLVED) {
            return workOrders.findAll().stream()
                    .filter(w -> w.getHotspot() != null
                            && w.getHotspot().getId().equals(hotspot.getId()))
                    .findFirst()
                    .orElseGet(() -> createOrder(hotspot));
        }
        return createOrder(hotspot);
    }

    private WorkOrder createOrder(Hotspot h) {
        WorkOrder w = new WorkOrder();
        w.setHotspot(h);
        w.setPriority(priorityFor(h));
        w.setStatus(WorkOrder.Status.PENDING);
        w.setCrew(routeCrew(h));
        w.setSummary(String.format("Cluster of %d reports - severity %.1f at %s",
                h.getReportCount(),
                h.getSeverity() == null ? 0 : h.getSeverity(),
                h.getLabel() == null ? "Leuven" : h.getLabel()));
        w.setCreatedAt(Instant.now());
        WorkOrder saved = workOrders.save(w);

        // Mock the actual Planon REST call.
        String ref = callPlanonRest(saved);
        saved.setPlanonRef(ref);
        saved.setStatus(WorkOrder.Status.DISPATCHED);
        saved.setDispatchedAt(Instant.now());

        h.setStatus(Hotspot.Status.DISPATCHED);
        h.setLastUpdatedAt(Instant.now());
        hotspots.save(h);

        return workOrders.save(saved);
    }

    private String callPlanonRest(WorkOrder w) {
        // Real impl:
        //   RestClient.create(props.getPlanon().getBaseUrl())
        //       .post()
        //       .uri("/Order/Create")
        //       .header("Authorization", "Bearer " + props.getPlanon().getApiKey())
        //       .body(map)
        //       .retrieve()
        //       .body(PlanonResponse.class);
        log.info("[PLANON-MOCK] dispatching workOrder={} priority={} crew={}",
                w.getId(), w.getPriority(), w.getCrew());
        return "PLN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private WorkOrder.Priority priorityFor(Hotspot h) {
        double sev = h.getSeverity() == null ? 3 : h.getSeverity();
        int count = h.getReportCount() == null ? 1 : h.getReportCount();
        if (sev <= 1.0 || count >= 6) return WorkOrder.Priority.URGENT;
        if (sev <= 2.0 || count >= 4) return WorkOrder.Priority.HIGH;
        if (sev <= 3.0) return WorkOrder.Priority.MEDIUM;
        return WorkOrder.Priority.LOW;
    }

    private String routeCrew(Hotspot h) {
        // toy "routing": use street segment district if known
        if (h.getStreetSegment() != null && h.getStreetSegment().getDistrict() != null) {
            return "Crew-" + h.getStreetSegment().getDistrict();
        }
        return "Crew-Centrum";
    }
}
