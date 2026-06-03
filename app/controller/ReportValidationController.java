package app.controller;

import app.dto.MonthlyReportResponseDto;
import app.dto.MonthlyReportSummaryDto;
import app.dto.ValidateReportRequest;
import app.service.ReportValidationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReportValidationController {

    private final ReportValidationService validationService;

    public ReportValidationController(ReportValidationService validationService) {
        this.validationService = validationService;
    }

    /** Validation par l'étudiant — passe de DRAFT/REOPENED à STUDENT_VALIDATED. */
    @PostMapping("/api/reports/{id}/validate/student")
    public ResponseEntity<MonthlyReportResponseDto> validateStudent(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(validationService.validateByStudent(id, auth.getName()));
    }

    /** Validation par le formateur — passe à TRAINER_VALIDATED, commentaire optionnel. */
    @PostMapping("/api/reports/{id}/validate/trainer")
    public ResponseEntity<MonthlyReportResponseDto> validateTrainer(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ValidateReportRequest request,
            Authentication auth) {
        String comment = request != null ? request.comment() : null;
        return ResponseEntity.ok(validationService.validateByTrainer(id, auth.getName(), comment));
    }

    /** Validation par le tuteur — passe à TUTOR_VALIDATED puis COMPLETED, commentaire optionnel. */
    @PostMapping("/api/reports/{id}/validate/tutor")
    public ResponseEntity<MonthlyReportResponseDto> validateTutor(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ValidateReportRequest request,
            Authentication auth) {
        String comment = request != null ? request.comment() : null;
        return ResponseEntity.ok(validationService.validateByTutor(id, auth.getName(), comment));
    }

    /** Rapports en attente de validation pour le demandeur (TRAINER ou TUTOR). */
    @GetMapping("/api/reports/pending")
    public ResponseEntity<List<MonthlyReportSummaryDto>> getPending(Authentication auth) {
        return ResponseEntity.ok(validationService.getPendingReports(auth.getName()));
    }
}
