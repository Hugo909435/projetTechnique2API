package app.service;

import app.dto.MonthlyReportResponseDto;
import app.dto.MonthlyReportSummaryDto;
import app.exception.ForbiddenException;
import app.exception.ResourceNotFoundException;
import app.model.*;
import app.repository.MonthlyReportRepository;
import app.repository.StudentProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class ReportValidationService {

    private static final Logger log = LoggerFactory.getLogger(ReportValidationService.class);
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private final MonthlyReportRepository reportRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserService userService;
    private final MonthlyReportService reportService;
    private final EmailService emailService;

    public ReportValidationService(MonthlyReportRepository reportRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    UserService userService,
                                    MonthlyReportService reportService,
                                    EmailService emailService) {
        this.reportRepository = reportRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userService = userService;
        this.reportService = reportService;
        this.emailService = emailService;
    }

    // ── Validation étudiant ───────────────────────────────────────────────────

    public MonthlyReportResponseDto validateByStudent(Long reportId, String email) {
        User student = userService.requireUser(email);

        if (student.getRole() != Role.STUDENT)
            throw new ForbiddenException("Seul l'étudiant peut effectuer cette validation");

        MonthlyReport report = requireReport(reportId);

        if (!report.getStudent().getId().equals(student.getId()))
            throw new ForbiddenException("Vous ne pouvez valider que vos propres rapports");

        if (report.getStatus() != ReportStatus.DRAFT)
            throw new ForbiddenException(
                    "Ce rapport ne peut pas être validé (statut actuel : " + report.getStatus() + ")");

        ReportStatus from = report.getStatus();
        report.setStatus(ReportStatus.STUDENT_VALIDATED);
        report.setStudentValidatedAt(LocalDateTime.now());
        report.setValidatedByStudent(student);
        appendLog(report, from, ReportStatus.STUDENT_VALIDATED, student, null);
        reportRepository.save(report);

        notifyOnStatusChanged(report, student, from, ReportStatus.STUDENT_VALIDATED);
        return reportService.toResponseDto(requireReport(reportId));
    }

    // ── Auto-validation planifiée ─────────────────────────────────────────────

    /** Point d'entrée appelé par le scheduler. */
    public int autoValidateReports() {
        return autoValidateExpiredReports(LocalDate.now());
    }

    /** Package-private pour les tests (permet de simuler la date). */
    int autoValidateExpiredReports(LocalDate today) {
        if (today.getDayOfMonth() < 5) return 0;

        YearMonth prevMonth = YearMonth.from(today).minusMonths(1);
        List<MonthlyReport> toValidate = reportRepository.findByStatusAndYearAndMonth(
                ReportStatus.DRAFT, prevMonth.getYear(), prevMonth.getMonthValue());

        if (toValidate.isEmpty()) return 0;

        LocalDateTime now = LocalDateTime.now();
        for (MonthlyReport report : toValidate) {
            ReportStatus from = report.getStatus();
            report.setStatus(ReportStatus.AUTO_VALIDATED);
            report.setAutoValidatedAt(now);
            appendLog(report, from, ReportStatus.AUTO_VALIDATED, null,
                    "Validation automatique — délai de dépôt expiré");
        }
        reportRepository.saveAll(toValidate);
        toValidate.forEach(r ->
                notifyOnStatusChanged(r, null, ReportStatus.DRAFT, ReportStatus.AUTO_VALIDATED));

        return toValidate.size();
    }

    // ── Validation formateur ──────────────────────────────────────────────────

    public MonthlyReportResponseDto validateByTrainer(Long reportId, String email, String comment) {
        User trainer = userService.requireUser(email);

        if (trainer.getRole() != Role.TRAINER)
            throw new ForbiddenException("Seul le formateur peut effectuer cette validation");

        MonthlyReport report = requireReport(reportId);

        if (!studentProfileRepository.existsByStudentIdAndTrainerId(
                report.getStudent().getId(), trainer.getId()))
            throw new ForbiddenException("Ce rapport n'appartient pas à l'un de vos étudiants");

        if (report.getStatus() != ReportStatus.TUTOR_VALIDATED)
            throw new ForbiddenException(
                    "Le formateur ne peut valider qu'après le tuteur " +
                    "(statut actuel : " + report.getStatus() + ")");

        LocalDateTime now = LocalDateTime.now();
        ReportStatus from = report.getStatus();
        report.setStatus(ReportStatus.TRAINER_VALIDATED);
        report.setTrainerValidatedAt(now);
        report.setValidatedByTrainer(trainer);
        if (comment != null && !comment.isBlank()) report.setTrainerNote(comment.trim());
        appendLog(report, from, ReportStatus.TRAINER_VALIDATED, trainer, null);

        // TRAINER_VALIDATED → COMPLETED (immédiat)
        report.setStatus(ReportStatus.COMPLETED);
        report.setCompletedAt(now);
        appendLog(report, ReportStatus.TRAINER_VALIDATED, ReportStatus.COMPLETED, trainer, null);
        reportRepository.save(report);

        notifyOnStatusChanged(report, trainer, from, ReportStatus.COMPLETED);
        return reportService.toResponseDto(requireReport(reportId));
    }

    // ── Validation tuteur ─────────────────────────────────────────────────────

    public MonthlyReportResponseDto validateByTutor(Long reportId, String email, String comment) {
        User tutor = userService.requireUser(email);

        if (tutor.getRole() != Role.TUTOR)
            throw new ForbiddenException("Seul le tuteur peut effectuer cette validation");

        MonthlyReport report = requireReport(reportId);

        if (!studentProfileRepository.existsByStudentIdAndTutorId(
                report.getStudent().getId(), tutor.getId()))
            throw new ForbiddenException("Ce rapport n'appartient pas à l'un de vos alternants");

        if (report.getStatus() != ReportStatus.STUDENT_VALIDATED
                && report.getStatus() != ReportStatus.AUTO_VALIDATED)
            throw new ForbiddenException(
                    "Le tuteur ne peut valider qu'après la validation étudiant " +
                    "(statut actuel : " + report.getStatus() + ")");

        ReportStatus from = report.getStatus();
        report.setStatus(ReportStatus.TUTOR_VALIDATED);
        report.setTutorValidatedAt(LocalDateTime.now());
        report.setValidatedByTutor(tutor);
        if (comment != null && !comment.isBlank()) report.setTutorNote(comment.trim());
        appendLog(report, from, ReportStatus.TUTOR_VALIDATED, tutor, null);
        reportRepository.save(report);

        notifyOnStatusChanged(report, tutor, from, ReportStatus.TUTOR_VALIDATED);
        return reportService.toResponseDto(requireReport(reportId));
    }

    // ── Rapports en attente de validation ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MonthlyReportSummaryDto> getPendingReports(String email) {
        User requester = userService.requireUser(email);
        List<Long> studentIds;
        List<ReportStatus> statuses;

        switch (requester.getRole()) {
            case TUTOR -> {
                studentIds = studentIds(studentProfileRepository.findByTutorId(requester.getId()));
                statuses = List.of(ReportStatus.STUDENT_VALIDATED, ReportStatus.AUTO_VALIDATED);
            }
            case TRAINER -> {
                studentIds = studentIds(studentProfileRepository.findByTrainerId(requester.getId()));
                statuses = List.of(ReportStatus.TUTOR_VALIDATED);
            }
            default -> { return List.of(); }
        }

        if (studentIds.isEmpty()) return List.of();
        return reportRepository
                .findByStudentIdInAndStatusInOrderByYearDescMonthDesc(studentIds, statuses)
                .stream().map(this::toSummaryDto).toList();
    }

    // ── Notifications email ───────────────────────────────────────────────────

    private void notifyOnStatusChanged(MonthlyReport report, User actor,
                                        ReportStatus from, ReportStatus to) {
        try {
            switch (to) {
                case STUDENT_VALIDATED, AUTO_VALIDATED -> emailService.sendTutorValidationRequest(report);
                case TUTOR_VALIDATED                   -> emailService.sendTrainerValidationRequest(report);
                default -> {}
            }
        } catch (Exception e) {
            log.error("Erreur envoi email rapport {} (transition {} → {}) : {}",
                    report.getId(), from, to, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendLog(MonthlyReport report, ReportStatus from, ReportStatus to,
                            User changedBy, String note) {
        ReportStatusLog log = ReportStatusLog.builder()
                .report(report)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .note(note)
                .build();
        report.getStatusLogs().add(log);
    }

    private MonthlyReport requireReport(Long id) {
        return reportRepository.findByIdWithSections(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable : " + id));
    }

    private MonthlyReportSummaryDto toSummaryDto(MonthlyReport r) {
        return reportService.toSummaryDto(r);
    }

    private List<Long> studentIds(List<StudentProfile> profiles) {
        return profiles.stream().map(p -> p.getStudent().getId()).toList();
    }
}
