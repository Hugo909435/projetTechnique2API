package app.controller;

import app.dto.CreateMonthlyReportRequest;
import app.dto.MonthlyReportResponseDto;
import app.dto.MonthlyReportSummaryDto;
import app.dto.UpdateMonthlyReportRequest;
import app.service.MonthlyReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MonthlyReportController {

    private final MonthlyReportService reportService;

    public MonthlyReportController(MonthlyReportService reportService) {
        this.reportService = reportService;
    }

    // ── Rapports accessibles au demandeur ──────────────────────────────────────

    /** STUDENT → ses rapports. TRAINER/TUTOR → leurs étudiants. ADMIN → tous. */
    @GetMapping("/api/reports")
    public ResponseEntity<List<MonthlyReportSummaryDto>> getMyReports(Authentication auth) {
        return ResponseEntity.ok(reportService.getMyReports(auth.getName()));
    }

    /** Détail d'un rapport — contrôle d'accès dans le service. */
    @GetMapping("/api/reports/{id}")
    public ResponseEntity<MonthlyReportResponseDto> getReport(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(reportService.getReport(id, auth.getName()));
    }

    /** Création d'un rapport — STUDENT uniquement. */
    @PostMapping("/api/reports")
    public ResponseEntity<MonthlyReportResponseDto> createReport(
            @Valid @RequestBody CreateMonthlyReportRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createReport(request, auth.getName()));
    }

    /** Mise à jour des sections — DRAFT / REOPENED seulement, par l'étudiant. */
    @PutMapping("/api/reports/{id}")
    public ResponseEntity<MonthlyReportResponseDto> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMonthlyReportRequest request,
            Authentication auth) {
        return ResponseEntity.ok(reportService.updateReport(id, request, auth.getName()));
    }

    // ── Rapports d'un étudiant spécifique ──────────────────────────────────────

    /** Tous les rapports d'un étudiant. Accès : ADMIN, TRAINER, TUTOR (ownership), STUDENT (self). */
    @GetMapping("/api/students/{studentId}/reports")
    public ResponseEntity<List<MonthlyReportSummaryDto>> getStudentReports(
            @PathVariable Long studentId, Authentication auth) {
        return ResponseEntity.ok(reportService.getReportsForStudent(studentId, auth.getName()));
    }
}
