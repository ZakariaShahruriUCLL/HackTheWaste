package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.PendingReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingReportRepository extends JpaRepository<PendingReport, Long> {

    Optional<PendingReport> findFirstByCompletedFalseOrderByCreatedAtDesc();

    Optional<PendingReport> findFirstByFromNumberAndCompletedFalseOrderByCreatedAtDesc(
            String fromNumber);
}
