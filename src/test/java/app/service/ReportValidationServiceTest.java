package app.service;

import app.dto.MonthlyReportResponseDto;
import app.dto.MonthlyReportSummaryDto;
import app.dto.ReopenReportRequest;
import app.exception.ForbiddenException;
import app.model.*;
import app.repository.MonthlyReportRepository;
import app.repository.ReportCommentRepository;
import app.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportValidationServiceTest {

    @Mock MonthlyReportRepository reportRepository;
    @Mock ReportCommentRepository commentRepository;
    @Mock StudentProfileRepository studentProfileRepository;
    @Mock UserService userService;
    @Mock MonthlyReportService reportService;

    @InjectMocks ReportValidationService service;

    private User student;
    private User trainer;
    private User tutor;
    private MonthlyReport draftReport;
    private MonthlyReportResponseDto stubDto;

    @BeforeEach
    void setUp() {
        student = buildUser(1L, "student@test.com", Role.STUDENT, "Alice", "Durand");
        trainer = buildUser(2L, "trainer@test.com", Role.TRAINER, "Jean", "Dupont");
        tutor   = buildUser(3L, "tutor@test.com",   Role.TUTOR,   "Paul", "Martin");

        draftReport = buildReport(10L, student, 2024, 5, ReportStatus.DRAFT);
        stubDto = buildStubDto();
    }

    // ── Validation étudiant ───────────────────────────────────────────────────

    @Nested
    class ValidateByStudent {

        @Test
        void student_can_validate_own_draft_report() {
            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(draftReport));
            when(reportRepository.save(any())).thenReturn(draftReport);
            when(reportService.toResponseDto(any())).thenReturn(stubDto);

            service.validateByStudent(10L, "student@test.com");

            assertThat(draftReport.getStatus()).isEqualTo(ReportStatus.STUDENT_VALIDATED);
            assertThat(draftReport.getStudentValidatedAt()).isNotNull();
            assertThat(draftReport.getValidatedByStudent()).isEqualTo(student);
        }

        @Test
        void student_can_validate_reopened_report() {
            MonthlyReport reopened = buildReport(10L, student, 2024, 5, ReportStatus.REOPENED);
            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(reopened));
            when(reportRepository.save(any())).thenReturn(reopened);
            when(reportService.toResponseDto(any())).thenReturn(stubDto);

            service.validateByStudent(10L, "student@test.com");

            assertThat(reopened.getStatus()).isEqualTo(ReportStatus.STUDENT_VALIDATED);
        }

        @Test
        void trainer_cannot_use_student_validation() {
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);

            assertThatThrownBy(() -> service.validateByStudent(10L, "trainer@test.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("étudiant");
        }

        @Test
        void cannot_validate_already_validated_report() {
            MonthlyReport validated = buildReport(10L, student, 2024, 5, ReportStatus.STUDENT_VALIDATED);
            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(validated));

            assertThatThrownBy(() -> service.validateByStudent(10L, "student@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── Auto-validation ───────────────────────────────────────────────────────

    @Nested
    class AutoValidate {

        @Test
        void no_validation_before_day_5() {
            int count = service.autoValidateExpiredReports(LocalDate.of(2024, 6, 4));

            assertThat(count).isZero();
            verifyNoInteractions(reportRepository);
        }

        @Test
        void validates_draft_reports_from_previous_month_on_day_5() {
            MonthlyReport r1 = buildReport(11L, student, 2024, 5, ReportStatus.DRAFT);
            MonthlyReport r2 = buildReport(12L, student, 2024, 5, ReportStatus.DRAFT);

            when(reportRepository.findByStatusAndYearAndMonth(ReportStatus.DRAFT, 2024, 5))
                    .thenReturn(List.of(r1, r2));
            when(reportRepository.saveAll(any())).thenReturn(List.of(r1, r2));

            int count = service.autoValidateExpiredReports(LocalDate.of(2024, 6, 5));

            assertThat(count).isEqualTo(2);
            assertThat(r1.getStatus()).isEqualTo(ReportStatus.AUTO_VALIDATED);
            assertThat(r2.getStatus()).isEqualTo(ReportStatus.AUTO_VALIDATED);
            assertThat(r1.getAutoValidatedAt()).isNotNull();
        }

        @Test
        void no_draft_reports_found_returns_zero() {
            when(reportRepository.findByStatusAndYearAndMonth(ReportStatus.DRAFT, 2024, 5))
                    .thenReturn(List.of());

            int count = service.autoValidateExpiredReports(LocalDate.of(2024, 6, 10));

            assertThat(count).isZero();
            verify(reportRepository, never()).saveAll(any());
        }
    }

    // ── Validation formateur ──────────────────────────────────────────────────

    @Nested
    class ValidateByTrainer {

        @Test
        void trainer_validates_student_validated_report() {
            MonthlyReport report = buildReport(10L, student, 2024, 5, ReportStatus.STUDENT_VALIDATED);
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(report));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);
            when(reportRepository.save(any())).thenReturn(report);
            when(reportService.toResponseDto(any())).thenReturn(stubDto);

            service.validateByTrainer(10L, "trainer@test.com", null);

            assertThat(report.getStatus()).isEqualTo(ReportStatus.TRAINER_VALIDATED);
            assertThat(report.getTrainerValidatedAt()).isNotNull();
            assertThat(report.getValidatedByTrainer()).isEqualTo(trainer);
        }

        @Test
        void trainer_validates_auto_validated_report() {
            MonthlyReport report = buildReport(10L, student, 2024, 5, ReportStatus.AUTO_VALIDATED);
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(report));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);
            when(reportRepository.save(any())).thenReturn(report);
            when(reportService.toResponseDto(any())).thenReturn(stubDto);

            service.validateByTrainer(10L, "trainer@test.com", null);

            assertThat(report.getStatus()).isEqualTo(ReportStatus.TRAINER_VALIDATED);
        }

        @Test
        void trainer_cannot_validate_before_student() {
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(draftReport));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> service.validateByTrainer(10L, "trainer@test.com", null))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("validation étudiant");
        }

        @Test
        void trainer_with_comment_saves_internal_comment() {
            MonthlyReport report = buildReport(10L, student, 2024, 5, ReportStatus.STUDENT_VALIDATED);
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(report));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);
            when(reportRepository.save(any())).thenReturn(report);
            when(reportService.toResponseDto(any())).thenReturn(stubDto);

            service.validateByTrainer(10L, "trainer@test.com", "Bon rapport");

            verify(commentRepository).save(argThat(c -> "Bon rapport".equals(c.getContent())));
        }
    }

    // ── Validation tuteur ─────────────────────────────────────────────────────

    @Nested
    class ValidateByTutor {

        @Test
        void tutor_validates_trainer_validated_report_and_completes_it() {
            MonthlyReport report = buildReport(10L, student, 2024, 5, ReportStatus.TRAINER_VALIDATED);
            when(userService.requireUser("tutor@test.com")).thenReturn(tutor);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(report));
            when(studentProfileRepository.existsByStudentIdAndTutorId(1L, 3L)).thenReturn(true);
            when(reportRepository.save(any())).thenReturn(report);
            when(reportService.toResponseDto(any())).thenReturn(stubDto);

            service.validateByTutor(10L, "tutor@test.com", null);

            assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
            assertThat(report.getTutorValidatedAt()).isNotNull();
            assertThat(report.getCompletedAt()).isNotNull();
        }

        @Test
        void tutor_cannot_validate_before_trainer() {
            MonthlyReport report = buildReport(10L, student, 2024, 5, ReportStatus.STUDENT_VALIDATED);
            when(userService.requireUser("tutor@test.com")).thenReturn(tutor);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(report));
            when(studentProfileRepository.existsByStudentIdAndTutorId(1L, 3L)).thenReturn(true);

            assertThatThrownBy(() -> service.validateByTutor(10L, "tutor@test.com", null))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("formateur");
        }

        @Test
        void tutor_with_comment_saves_internal_comment() {
            MonthlyReport report = buildReport(10L, student, 2024, 5, ReportStatus.TRAINER_VALIDATED);
            when(userService.requireUser("tutor@test.com")).thenReturn(tutor);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(report));
            when(studentProfileRepository.existsByStudentIdAndTutorId(1L, 3L)).thenReturn(true);
            when(reportRepository.save(any())).thenReturn(report);
            when(reportService.toResponseDto(any())).thenReturn(stubDto);

            service.validateByTutor(10L, "tutor@test.com", "Très bon travail");

            verify(commentRepository).save(argThat(c -> "Très bon travail".equals(c.getContent())));
        }
    }

    // ── Rapports en attente ───────────────────────────────────────────────────

    @Nested
    class GetPendingReports {

        @Test
        void trainer_gets_student_and_auto_validated_reports() {
            MonthlyReport pending = buildReport(20L, student, 2024, 5, ReportStatus.STUDENT_VALIDATED);
            StudentProfile profile = buildProfile(student);
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(studentProfileRepository.findByTrainerId(2L)).thenReturn(List.of(profile));
            when(reportRepository.findByStudentIdInAndStatusInOrderByYearDescMonthDesc(
                    List.of(1L),
                    List.of(ReportStatus.STUDENT_VALIDATED, ReportStatus.AUTO_VALIDATED)))
                    .thenReturn(List.of(pending));

            List<MonthlyReportSummaryDto> result = service.getPendingReports("trainer@test.com");

            assertThat(result).hasSize(1);
        }

        @Test
        void tutor_gets_trainer_validated_reports() {
            StudentProfile profile = buildProfile(student);
            when(userService.requireUser("tutor@test.com")).thenReturn(tutor);
            when(studentProfileRepository.findByTutorId(3L)).thenReturn(List.of(profile));
            when(reportRepository.findByStudentIdInAndStatusInOrderByYearDescMonthDesc(
                    List.of(1L), List.of(ReportStatus.TRAINER_VALIDATED)))
                    .thenReturn(List.of());

            List<MonthlyReportSummaryDto> result = service.getPendingReports("tutor@test.com");

            assertThat(result).isEmpty();
        }

        @Test
        void student_gets_empty_list() {
            when(userService.requireUser("student@test.com")).thenReturn(student);

            List<MonthlyReportSummaryDto> result = service.getPendingReports("student@test.com");

            assertThat(result).isEmpty();
            verifyNoInteractions(reportRepository);
        }
    }

    // ── Réouverture ───────────────────────────────────────────────────────────

    @Nested
    class ReopenReport {

        @Test
        void trainer_can_reopen_student_validated_report() {
            MonthlyReport report = buildReport(10L, student, 2024, 5, ReportStatus.STUDENT_VALIDATED);
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(report));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);
            when(reportRepository.save(any())).thenReturn(report);
            when(reportService.toResponseDto(any())).thenReturn(stubDto);

            service.reopenReport(10L, new ReopenReportRequest("Corrections nécessaires"),
                    "trainer@test.com");

            assertThat(report.getStatus()).isEqualTo(ReportStatus.REOPENED);
        }

        @Test
        void student_cannot_reopen_report() {
            when(userService.requireUser("student@test.com")).thenReturn(student);

            assertThatThrownBy(() -> service.reopenReport(10L, null, "student@test.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("formateur");
        }

        @Test
        void completed_report_cannot_be_reopened() {
            MonthlyReport completed = buildReport(10L, student, 2024, 5, ReportStatus.COMPLETED);
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(completed));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> service.reopenReport(10L, null, "trainer@test.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("rouvert");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(Long id, String email, Role role, String first, String last) {
        return User.builder().id(id).email(email).role(role)
                .firstName(first).lastName(last).password("hash").build();
    }

    private MonthlyReport buildReport(Long id, User student, int year, int month, ReportStatus status) {
        MonthlyReport r = new MonthlyReport();
        r.setId(id);
        r.setStudent(student);
        r.setYear(year);
        r.setMonth(month);
        r.setStatus(status);
        r.setSections(new LinkedHashSet<>());
        r.setStatusLogs(new LinkedHashSet<>());
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private StudentProfile buildProfile(User student) {
        return StudentProfile.builder().id(1L).student(student).build();
    }

    private MonthlyReportResponseDto buildStubDto() {
        return new MonthlyReportResponseDto(
                10L, 2024, 5, "mai 2024", "STUDENT_VALIDATED", false,
                1L, "Alice Durand",
                List.of(), List.of(),
                null, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
