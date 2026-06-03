package app.service;

import app.exception.ForbiddenException;
import app.model.*;
import app.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportPermissionServiceTest {

    @Mock StudentProfileRepository studentProfileRepository;

    @InjectMocks ReportPermissionService service;

    private User student;
    private User otherStudent;
    private User trainer;
    private User otherTrainer;
    private User tutor;
    private User admin;
    private MonthlyReport report;

    @BeforeEach
    void setUp() {
        student      = user(1L, Role.STUDENT,  "Alice", "Durand");
        otherStudent = user(9L, Role.STUDENT,  "Bob",   "Other");
        trainer      = user(2L, Role.TRAINER,  "Jean",  "Dupont");
        otherTrainer = user(99L, Role.TRAINER, "Paul",  "Autre");
        tutor        = user(3L, Role.TUTOR,    "Pierre","Martin");
        admin        = user(4L, Role.ADMIN,    "Admin", "System");

        report = new MonthlyReport();
        report.setStudent(student);
        report.setStatus(ReportStatus.DRAFT);
    }

    // ── assertCanRead ─────────────────────────────────────────────────────────

    @Test void admin_can_read_any_report() {
        assertThatNoException().isThrownBy(() -> service.assertCanRead(report, admin));
    }

    @Test void student_can_read_own_report() {
        assertThatNoException().isThrownBy(() -> service.assertCanRead(report, student));
    }

    @Test void student_cannot_read_other_student_report() {
        assertThatThrownBy(() -> service.assertCanRead(report, otherStudent))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test void trainer_can_read_own_student_report() {
        when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.assertCanRead(report, trainer));
    }

    @Test void trainer_cannot_read_other_trainer_student_report() {
        when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 99L)).thenReturn(false);
        assertThatThrownBy(() -> service.assertCanRead(report, otherTrainer))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test void tutor_can_read_own_student_report() {
        when(studentProfileRepository.existsByStudentIdAndTutorId(1L, 3L)).thenReturn(true);
        assertThatNoException().isThrownBy(() -> service.assertCanRead(report, tutor));
    }

    // ── assertCanEdit ─────────────────────────────────────────────────────────

    @Test void student_can_edit_draft_report() {
        assertThatNoException().isThrownBy(() -> service.assertCanEdit(report, student));
    }

    @Test void student_cannot_edit_validated_report() {
        report.setStatus(ReportStatus.STUDENT_VALIDATED);
        assertThatThrownBy(() -> service.assertCanEdit(report, student))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("modifiable");
    }

    @Test void trainer_cannot_edit_report() {
        when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.assertCanEdit(report, trainer))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private User user(Long id, Role role, String first, String last) {
        return User.builder().id(id).role(role).firstName(first).lastName(last)
                .email(first.toLowerCase() + "@test.com").password("h").build();
    }
}
