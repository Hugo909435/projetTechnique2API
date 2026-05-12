package app.service;

import app.dto.*;
import app.exception.ConflictException;
import app.exception.ForbiddenException;
import app.exception.ResourceNotFoundException;
import app.model.*;
import app.repository.MonthlyReportRepository;
import app.repository.ReportSectionRepository;
import app.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MonthlyReportService {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private final MonthlyReportRepository reportRepository;
    private final ReportSectionRepository sectionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserService userService;
    private final ReportPermissionService permissionService;

    public MonthlyReportService(MonthlyReportRepository reportRepository,
                                ReportSectionRepository sectionRepository,
                                StudentProfileRepository studentProfileRepository,
                                UserService userService,
                                ReportPermissionService permissionService) {
        this.reportRepository = reportRepository;
        this.sectionRepository = sectionRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    // ── Création ──────────────────────────────────────────────────────────────

    public MonthlyReportResponseDto createReport(CreateMonthlyReportRequest req, String studentEmail) {
        User student = userService.requireUser(studentEmail);

        if (student.getRole() != Role.STUDENT)
            throw new ForbiddenException("Seul un étudiant peut créer un rapport");

        if (reportRepository.existsByStudentIdAndYearAndMonth(student.getId(), req.year(), req.month()))
            throw new ConflictException("Un rapport existe déjà pour " + monthLabel(req.month(), req.year()));

        MonthlyReport report = reportRepository.save(
                MonthlyReport.builder()
                        .student(student)
                        .year(req.year())
                        .month(req.month())
                        .status(ReportStatus.DRAFT)
                        .build());

        // 6 sections vides dans l'ordre de l'enum
        List<ReportSection> sections = Arrays.stream(ReportSectionType.values())
                .map(type -> ReportSection.builder()
                        .report(report)
                        .sectionType(type)
                        .content("")
                        .build())
                .collect(Collectors.toList());
        sectionRepository.saveAll(sections);

        // Entrée dans l'historique
        appendLog(report, null, ReportStatus.DRAFT, student, null);

        return toResponseDto(requireWithDetails(report.getId()));
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MonthlyReportResponseDto getReport(Long id, String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        MonthlyReport report = requireWithDetails(id);
        permissionService.assertCanRead(report, requester);
        return toResponseDto(report);
    }

    @Transactional(readOnly = true)
    public List<MonthlyReportSummaryDto> getMyReports(String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);

        List<MonthlyReport> reports = switch (requester.getRole()) {
            case STUDENT ->
                reportRepository.findByStudentIdOrderByYearDescMonthDesc(requester.getId());
            case TRAINER -> {
                List<Long> ids = studentIds(studentProfileRepository.findByTrainerId(requester.getId()));
                yield ids.isEmpty() ? List.of()
                        : reportRepository.findByStudentIdInOrderByYearDescMonthDesc(ids);
            }
            case TUTOR -> {
                List<Long> ids = studentIds(studentProfileRepository.findByTutorId(requester.getId()));
                yield ids.isEmpty() ? List.of()
                        : reportRepository.findByStudentIdInOrderByYearDescMonthDesc(ids);
            }
            case ADMIN ->
                reportRepository.findAll().stream()
                        .sorted(Comparator.comparingInt(MonthlyReport::getYear)
                                .thenComparingInt(MonthlyReport::getMonth).reversed())
                        .toList();
        };

        return reports.stream().map(this::toSummaryDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyReportSummaryDto> getReportsForStudent(Long studentId, String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        permissionService.assertCanReadStudentReports(studentId, requester);
        return reportRepository.findByStudentIdOrderByYearDescMonthDesc(studentId)
                .stream().map(this::toSummaryDto).toList();
    }

    // ── Mise à jour des sections ──────────────────────────────────────────────

    public MonthlyReportResponseDto updateReport(Long id, UpdateMonthlyReportRequest req,
                                                  String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        MonthlyReport report = requireWithDetails(id);
        permissionService.assertCanEdit(report, requester);

        Map<ReportSectionType, ReportSection> byType = report.getSections().stream()
                .collect(Collectors.toMap(ReportSection::getSectionType, s -> s));

        for (UpdateMonthlyReportRequest.SectionUpdate update : req.sections()) {
            ReportSectionType type = parseType(update.sectionType());
            ReportSection section = byType.get(type);
            if (section != null)
                section.setContent(update.content() != null ? update.content() : "");
        }

        sectionRepository.saveAll(byType.values());
        return toResponseDto(requireWithDetails(id));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Ajoute une entrée dans l'historique des statuts et cascade via le rapport. */
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
        reportRepository.save(report); // cascade ALL => log persisté
    }

    private MonthlyReport requireWithDetails(Long id) {
        return reportRepository.findByIdWithSections(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable : " + id));
    }

    private List<Long> studentIds(List<StudentProfile> profiles) {
        return profiles.stream()
                .map(p -> p.getStudent().getId())
                .toList();
    }

    // ── Conversion en DTO ──────────────────────────────────────────────────────

    /** Visible du package pour que ReportValidationService puisse l'utiliser. */
    MonthlyReportResponseDto toResponseDto(MonthlyReport r) {
        List<ReportSectionDto> sections = r.getSections().stream()
                .sorted(Comparator.comparingInt(s -> s.getSectionType().ordinal()))
                .map(s -> new ReportSectionDto(
                        s.getId(),
                        s.getSectionType().name(),
                        s.getSectionType().getLabel(),
                        s.getContent()))
                .toList();

        List<ReportStatusLogDto> logs = r.getStatusLogs().stream()
                .map(l -> new ReportStatusLogDto(
                        l.getFromStatus() != null ? l.getFromStatus().name() : null,
                        l.getToStatus().name(),
                        l.getChangedBy() != null
                                ? l.getChangedBy().getFirstName() + " " + l.getChangedBy().getLastName()
                                : "Système",
                        l.getChangedAt(),
                        l.getNote()))
                .toList();

        return new MonthlyReportResponseDto(
                r.getId(), r.getYear(), r.getMonth(),
                monthLabel(r.getMonth(), r.getYear()),
                r.getStatus().name(),
                r.getStatus().isEditable(),
                r.getStudent().getId(),
                r.getStudent().getFirstName() + " " + r.getStudent().getLastName(),
                sections, logs,
                r.getStudentValidatedAt(),
                r.getAutoValidatedAt(),
                r.getTrainerValidatedAt(),
                r.getTutorValidatedAt(),
                r.getCompletedAt(),
                r.getCreatedAt(), r.getUpdatedAt());
    }

    public MonthlyReportSummaryDto toSummaryDto(MonthlyReport r) {
        return new MonthlyReportSummaryDto(
                r.getId(), r.getYear(), r.getMonth(),
                monthLabel(r.getMonth(), r.getYear()),
                r.getStatus().name(),
                r.getStudent().getId(),
                r.getStudent().getFirstName() + " " + r.getStudent().getLastName(),
                r.getCreatedAt(), r.getUpdatedAt());
    }

    private String monthLabel(int month, int year) {
        return YearMonth.of(year, month).format(MONTH_FORMATTER);
    }

    private ReportSectionType parseType(String raw) {
        try {
            return ReportSectionType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Type de section invalide : " + raw);
        }
    }
}
