package app.service;

import app.dto.MonthlyReportResponseDto;
import app.dto.MonthlyReportSummaryDto;
import app.dto.ReopenReportRequest;
import app.exception.ForbiddenException;
import app.exception.ResourceNotFoundException;
import app.model.*;
import app.repository.MonthlyReportRepository;
import app.repository.StudentProfileRepository;
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

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private final MonthlyReportRepository reportRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserService userService;
    private final MonthlyReportService reportService;

    public ReportValidationService(MonthlyReportRepository reportRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    UserService userService,
                                    MonthlyReportService reportService) {
        this.reportRepository = reportRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userService = userService;
        this.reportService = reportService;
    }

    // ── Validation étudiant ───────────────────────────────────────────────────

    public MonthlyReportResponseDto validateByStudent(Long reportId, String email) {
        User student = userService.requireUser(email);

        if (student.getRole() != Role.STUDENT)
            throw new ForbiddenException("Seul l'étudiant peut effectuer cette validation");

        MonthlyReport report = requireReport(reportId);

        if (!report.getStudent().getId().equals(student.getId()))
            throw new ForbiddenException("Vous ne pouvez valider que vos propres rapports");

        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.REOPENED)
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

        if (report.getStatus() != ReportStatus.STUDENT_VALIDATED
                && report.getStatus() != ReportStatus.AUTO_VALIDATED)
            throw new ForbiddenException(
                    "Le formateur ne peut valider qu'après la validation étudiant " +
                    "(statut actuel : " + report.getStatus() + ")");

        ReportStatus from = report.getStatus();
        report.setStatus(ReportStatus.TRAINER_VALIDATED);
        report.setTrainerValidatedAt(LocalDateTime.now());
        report.setValidatedByTrainer(trainer);
        if (comment != null && !comment.isBlank()) report.setTrainerNote(comment.trim());
        appendLog(report, from, ReportStatus.TRAINER_VALIDATED, trainer, null);
        reportRepository.save(report);

        notifyOnStatusChanged(report, trainer, from, ReportStatus.TRAINER_VALIDATED);
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

        if (report.getStatus() != ReportStatus.TRAINER_VALIDATED)
            throw new ForbiddenException(
                    "Le tuteur ne peut valider qu'après le formateur " +
                    "(statut actuel : " + report.getStatus() + ")");

        LocalDateTime now = LocalDateTime.now();

        // TRAINER_VALIDATED → TUTOR_VALIDATED
        report.setStatus(ReportStatus.TUTOR_VALIDATED);
        report.setTutorValidatedAt(now);
        report.setValidatedByTutor(tutor);
        if (comment != null && !comment.isBlank()) report.setTutorNote(comment.trim());
        appendLog(report, ReportStatus.TRAINER_VALIDATED, ReportStatus.TUTOR_VALIDATED, tutor, null);

        // TUTOR_VALIDATED → COMPLETED (immédiat)
        report.setStatus(ReportStatus.COMPLETED);
        report.setCompletedAt(now);
        appendLog(report, ReportStatus.TUTOR_VALIDATED, ReportStatus.COMPLETED, tutor, null);
        reportRepository.save(report);

        notifyOnStatusChanged(report, tutor, ReportStatus.TUTOR_VALIDATED, ReportStatus.COMPLETED);
        return reportService.toResponseDto(requireReport(reportId));
    }

    // ── Réouverture ───────────────────────────────────────────────────────────

    public MonthlyReportResponseDto reopenReport(Long reportId, ReopenReportRequest req,
                                                  String email) {
        User requester = userService.requireUser(email);

        if (requester.getRole() != Role.TRAINER && requester.getRole() != Role.ADMIN)
            throw new ForbiddenException("Seul un formateur ou un admin peut rouvrir un rapport");

        MonthlyReport report = requireReport(reportId);

        if (requester.getRole() == Role.TRAINER
                && !studentProfileRepository.existsByStudentIdAndTrainerId(
                        report.getStudent().getId(), requester.getId()))
            throw new ForbiddenException("Ce rapport n'appartient pas à l'un de vos étudiants");

        if (!report.getStatus().canBeReopened())
            throw new ForbiddenException(
                    "Ce rapport ne peut pas être rouvert (statut : " + report.getStatus() + ")");

        ReportStatus from = report.getStatus();
        report.setStatus(ReportStatus.REOPENED);
        appendLog(report, from, ReportStatus.REOPENED, requester,
                req != null ? req.note() : null);
        reportRepository.save(report);

        notifyOnStatusChanged(report, requester, from, ReportStatus.REOPENED);
        return reportService.toResponseDto(requireReport(reportId));
    }

    // ── Rapports en attente de validation ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MonthlyReportSummaryDto> getPendingReports(String email) {
        User requester = userService.requireUser(email);
        List<Long> studentIds;
        List<ReportStatus> statuses;

        switch (requester.getRole()) {
            case TRAINER -> {
                studentIds = studentIds(studentProfileRepository.findByTrainerId(requester.getId()));
                statuses = List.of(ReportStatus.STUDENT_VALIDATED, ReportStatus.AUTO_VALIDATED);
            }
            case TUTOR -> {
                studentIds = studentIds(studentProfileRepository.findByTutorId(requester.getId()));
                statuses = List.of(ReportStatus.TRAINER_VALIDATED);
            }
            default -> { return List.of(); }
        }

        if (studentIds.isEmpty()) return List.of();
        return reportRepository
                .findByStudentIdInAndStatusInOrderByYearDescMonthDesc(studentIds, statuses)
                .stream().map(this::toSummaryDto).toList();
    }

    // ── Hook notifications (extensible ultérieurement) ────────────────────────

    /**
     * Appelé après chaque transition de statut.
     * À implémenter dans un module mail/notification ultérieur.
     *
     * @param report  le rapport dont le statut a changé
     * @param actor   l'utilisateur à l'origine du changement (null si auto-validation)
     * @param from    statut précédent
     * @param to      nouveau statut
     */
    private void notifyOnStatusChanged(MonthlyReport report, User actor,
                                        ReportStatus from, ReportStatus to) {
        // TODO : envoyer email/notification selon la transition
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
