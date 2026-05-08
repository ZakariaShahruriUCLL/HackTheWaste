package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("select r from Report r where r.reportedAt >= :since")
    List<Report> findRecent(Instant since);

    List<Report> findTop50ByOrderByReportedAtDesc();
}
