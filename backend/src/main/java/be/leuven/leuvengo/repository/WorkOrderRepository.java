package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findTop20ByOrderByCreatedAtDesc();
    List<WorkOrder> findByStatus(WorkOrder.Status status);
}
