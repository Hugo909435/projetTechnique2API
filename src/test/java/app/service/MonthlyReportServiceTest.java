package app.service;

import app.dto.CreateMonthlyReportRequest;
import app.dto.UpdateMonthlyReportRequest;
import app.exception.ConflictException;
import app.exception.ForbiddenException;
import app.model.*;
import app.repository.MonthlyReportRepository;
import app.repository.ReportSectionRepository;
import app.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyReportServiceTest {

    @Mock MonthlyReportRepository reportRepository;
    @Mock ReportSectionRepository sectionRepository;
    @Mock StudentProfileRepository studentProfileRepository;
    @Mock UserService userService;
    @Mock ReportPermissionService permissionService;

    @InjectMocks MonthlyReportService service;

    private User student;
    private User trainer;
    private MonthlyReport draftReport;

    @BeforeEach
    void setUp() {
        student = buildUser(1L, "student@test.com", Role.STUDENT, "Alice", "Durand");
        trainer = buildUser(2L, "trainer@test.com", Role.TRAINER, "Jean", "Dupont");

        draftReport = buildReport(10L, student, 2024, 5, ReportStatus.DRAFT);
    }

    // ── Création ─────────────────────────────────────────────────────────────

    @Nested
    class CreateReport {

        @Test
        void student_can_create_report() {
            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.existsByStudentIdAndYearAndMonth(1L, 2024, 5)).thenReturn(false);
            when(reportRepository.save(any())).thenReturn(draftReport);
            when(sectionRepository.saveAll(any())).thenReturn(List.of());
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(draftReport));

            var result = service.createReport(new CreateMonthlyReportRequest(2024, 5), "student@test.com");

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("DRAFT");
            verify(sectionRepository).saveAll(argThat(sections ->
                    ((List<?>) sections).size() == ReportSectionType.values().length));
        }

        @Test
        void cannot_create_duplicate_report() {
            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.existsByStudentIdAndYearAndMonth(1L, 2024, 5)).thenReturn(true);

            assertThatThrownBy(() ->
                    service.createReport(new CreateMonthlyReportRequest(2024, 5), "student@test.com"))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void trainer_cannot_create_report() {
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);

            assertThatThrownBy(() ->
                    service.createReport(new CreateMonthlyReportRequest(2024, 5), "trainer@test.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("étudiant");
        }
    }

    // ── Mise à jour ───────────────────────────────────────────────────────────

    @Nested
    class UpdateReport {

        @Test
        void student_can_update_draft_report() {
            ReportSection section = buildSection(draftReport, ReportSectionType.SCHOOL_ACTIVITIES, "old");
            draftReport.getSections().add(section);

            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findByIdWithSections(10L))
                    .thenReturn(Optional.of(draftReport));
            doNothing().when(permissionService).assertCanEdit(draftReport, student);
            when(sectionRepository.saveAll(any())).thenReturn(List.of());

            var update = new UpdateMonthlyReportRequest(List.of(
                    new UpdateMonthlyReportRequest.SectionUpdate("SCHOOL_ACTIVITIES", "new content")));

            service.updateReport(10L, update, "student@test.com");

            assertThat(section.getContent()).isEqualTo("new content");
        }

        @Test
        void update_on_validated_report_throws() {
            MonthlyReport validated = buildReport(10L, student, 2024, 5, ReportStatus.STUDENT_VALIDATED);
            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findByIdWithSections(10L)).thenReturn(Optional.of(validated));
            doThrow(new ForbiddenException("Non modifiable"))
                    .when(permissionService).assertCanEdit(validated, student);

            assertThatThrownBy(() -> service.updateReport(10L,
                    new UpdateMonthlyReportRequest(List.of()), "student@test.com"))
                    .isInstanceOf(ForbiddenException.class);
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
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private ReportSection buildSection(MonthlyReport report, ReportSectionType type, String content) {
        return ReportSection.builder().id(1L).report(report).sectionType(type).content(content).build();
    }
}
