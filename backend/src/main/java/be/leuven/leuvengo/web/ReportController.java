package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.Report;
import be.leuven.leuvengo.repository.ReportRepository;
import be.leuven.leuvengo.service.AuthService;
import be.leuven.leuvengo.service.ReportService;
import be.leuven.leuvengo.web.dto.CreateReportRequest;
import be.leuven.leuvengo.web.dto.Dtos;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportRepository reports;
    private final AuthService authService;

    public ReportController(ReportService reportService, ReportRepository reports,
                            AuthService authService) {
        this.reportService = reportService;
        this.reports = reports;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<Dtos.ReportDto> submit(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody CreateReportRequest req) {
        authService.requireAuth(auth);
        Report saved = reportService.submit(new ReportService.Submission(
                req.getLat(), req.getLng(), req.getRating(),
                req.getNote(), req.getImageRef(), req.getSignalTags(),
                req.getFacultyShortCode(), req.getReporterPseudoId()));
        return ResponseEntity.ok(Dtos.of(saved));
    }

    @GetMapping("/recent")
    public List<Dtos.ReportDto> recent() {
        return reports.findTop50ByOrderByReportedAtDesc().stream().map(Dtos::of).toList();
    }
}
