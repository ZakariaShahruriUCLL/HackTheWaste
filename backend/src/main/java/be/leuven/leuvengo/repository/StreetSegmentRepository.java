package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.StreetSegment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreetSegmentRepository extends JpaRepository<StreetSegment, Long> {
}
