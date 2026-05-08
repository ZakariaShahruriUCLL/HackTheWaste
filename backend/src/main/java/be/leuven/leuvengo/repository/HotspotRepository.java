package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.Hotspot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotspotRepository extends JpaRepository<Hotspot, Long> {
    List<Hotspot> findAllByStatusIn(List<Hotspot.Status> statuses);
}
