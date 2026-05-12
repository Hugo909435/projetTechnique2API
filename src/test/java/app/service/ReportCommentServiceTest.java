package app.service;

import app.dto.CreateReportCommentRequest;
import app.dto.ReportCommentResponseDto;
import app.dto.UpdateReportCommentRequest;
import app.exception.ForbiddenException;
import app.exception.ResourceNotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportCommentServiceTest {

    @Mock ReportCommentRepository commentRepository;
    @Mock MonthlyReportRepository reportRepository;
    @Mock StudentProfileRepository studentProfileRepository;
    @Mock UserService userService;

    @InjectMocks ReportCommentService service;

    private User student;
    private User trainer;
    private User tutor;
    private User admin;
    private MonthlyReport draftReport;
    private MonthlyReport completedReport;

    @BeforeEach
    void setUp() {
        student  = buildUser(1L, "student@test.com",  Role.STUDENT,  "Alice",  "Durand");
        trainer  = buildUser(2L, "trainer@test.com",  Role.TRAINER,  "Jean",   "Dupont");
        tutor    = buildUser(3L, "tutor@test.com",    Role.TUTOR,    "Marie",  "Martin");
        admin    = buildUser(4L, "admin@test.com",    Role.ADMIN,    "Super",  "Admin");

        draftReport     = buildReport(10L, student, ReportStatus.DRAFT);
        completedReport = buildReport(11L, student, ReportStatus.COMPLETED);
    }

    // ── GET comments — logique de visibilité ──────────────────────────────────

    @Nested
    class GetComments {

        @Test
        void student_gets_403() {
            when(userService.requireUser("student@test.com")).thenReturn(student);

            assertThatThrownBy(() -> service.getComments(10L, "student@test.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("étudiants");
        }

        @Test
        void trainer_sees_only_own_comments_before_completion() {
            ReportComment trainerComment = buildComment(1L, draftReport, trainer, Role.TRAINER, "note formateur");
            ReportComment tutorComment   = buildComment(2L, draftReport, tutor,   Role.TUTOR,   "note tuteur");

            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);
            when(commentRepository.findByReportIdOrderByCreatedAtAsc(10L))
                    .thenReturn(List.of(trainerComment, tutorComment));

            List<ReportCommentResponseDto> result = service.getComments(10L, "trainer@test.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).authorRole()).isEqualTo("TRAINER");
        }

        @Test
        void trainer_sees_tutor_comments_after_completion() {
            ReportComment trainerComment = buildComment(1L, completedReport, trainer, Role.TRAINER, "note formateur");
            ReportComment tutorComment   = buildComment(2L, completedReport, tutor,   Role.TUTOR,   "note tuteur");

            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findById(11L)).thenReturn(Optional.of(completedReport));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);
            when(commentRepository.findByReportIdOrderByCreatedAtAsc(11L))
                    .thenReturn(List.of(trainerComment, tutorComment));

            List<ReportCommentResponseDto> result = service.getComments(11L, "trainer@test.com");

            assertThat(result).hasSize(2);
        }

        @Test
        void tutor_sees_only_own_comments_before_completion() {
            ReportComment trainerComment = buildComment(1L, draftReport, trainer, Role.TRAINER, "note formateur");
            ReportComment tutorComment   = buildComment(2L, draftReport, tutor,   Role.TUTOR,   "note tuteur");

            when(userService.requireUser("tutor@test.com")).thenReturn(tutor);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            when(studentProfileRepository.existsByStudentIdAndTutorId(1L, 3L)).thenReturn(true);
            when(commentRepository.findByReportIdOrderByCreatedAtAsc(10L))
                    .thenReturn(List.of(trainerComment, tutorComment));

            List<ReportCommentResponseDto> result = service.getComments(10L, "tutor@test.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).authorRole()).isEqualTo("TUTOR");
        }

        @Test
        void tutor_sees_trainer_comments_after_completion() {
            ReportComment trainerComment = buildComment(1L, completedReport, trainer, Role.TRAINER, "note formateur");
            ReportComment tutorComment   = buildComment(2L, completedReport, tutor,   Role.TUTOR,   "note tuteur");

            when(userService.requireUser("tutor@test.com")).thenReturn(tutor);
            when(reportRepository.findById(11L)).thenReturn(Optional.of(completedReport));
            when(studentProfileRepository.existsByStudentIdAndTutorId(1L, 3L)).thenReturn(true);
            when(commentRepository.findByReportIdOrderByCreatedAtAsc(11L))
                    .thenReturn(List.of(trainerComment, tutorComment));

            List<ReportCommentResponseDto> result = service.getComments(11L, "tutor@test.com");

            assertThat(result).hasSize(2);
        }

        @Test
        void admin_sees_all_comments() {
            ReportComment trainerComment = buildComment(1L, draftReport, trainer, Role.TRAINER, "note formateur");
            ReportComment tutorComment   = buildComment(2L, draftReport, tutor,   Role.TUTOR,   "note tuteur");

            when(userService.requireUser("admin@test.com")).thenReturn(admin);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            when(commentRepository.findByReportIdOrderByCreatedAtAsc(10L))
                    .thenReturn(List.of(trainerComment, tutorComment));

            List<ReportCommentResponseDto> result = service.getComments(10L, "admin@test.com");

            assertThat(result).hasSize(2);
        }

        @Test
        void trainer_cannot_access_other_student_report() {
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(false);

            assertThatThrownBy(() -> service.getComments(10L, "trainer@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── POST — création ───────────────────────────────────────────────────────

    @Nested
    class CreateComment {

        @Test
        void trainer_can_create_comment_on_own_student_report() {
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(true);

            ReportComment saved = buildComment(1L, draftReport, trainer, Role.TRAINER, "super");
            when(commentRepository.save(any())).thenReturn(saved);

            ReportCommentResponseDto result = service.createComment(
                    10L, new CreateReportCommentRequest("super"), "trainer@test.com");

            assertThat(result.authorRole()).isEqualTo("TRAINER");
            assertThat(result.content()).isEqualTo("super");
        }

        @Test
        void student_cannot_create_comment() {
            when(userService.requireUser("student@test.com")).thenReturn(student);

            assertThatThrownBy(() ->
                    service.createComment(10L, new CreateReportCommentRequest("texte"), "student@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void admin_cannot_create_comment() {
            when(userService.requireUser("admin@test.com")).thenReturn(admin);

            assertThatThrownBy(() ->
                    service.createComment(10L, new CreateReportCommentRequest("texte"), "admin@test.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("formateur");
        }

        @Test
        void trainer_cannot_create_on_unrelated_student_report() {
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            when(studentProfileRepository.existsByStudentIdAndTrainerId(1L, 2L)).thenReturn(false);

            assertThatThrownBy(() ->
                    service.createComment(10L, new CreateReportCommentRequest("texte"), "trainer@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── PUT — modification ────────────────────────────────────────────────────

    @Nested
    class UpdateComment {

        @Test
        void author_can_update_own_comment() {
            ReportComment comment = buildComment(1L, draftReport, trainer, Role.TRAINER, "ancien");

            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
            when(commentRepository.save(any())).thenReturn(comment);

            ReportCommentResponseDto result = service.updateComment(
                    1L, new UpdateReportCommentRequest("nouveau"), "trainer@test.com");

            assertThat(comment.getContent()).isEqualTo("nouveau");
        }

        @Test
        void non_author_cannot_update() {
            ReportComment comment = buildComment(1L, draftReport, trainer, Role.TRAINER, "note");

            when(userService.requireUser("tutor@test.com")).thenReturn(tutor);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() ->
                    service.updateComment(1L, new UpdateReportCommentRequest("texte"), "tutor@test.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("propres commentaires");
        }

        @Test
        void student_gets_403_on_update() {
            when(userService.requireUser("student@test.com")).thenReturn(student);

            assertThatThrownBy(() ->
                    service.updateComment(1L, new UpdateReportCommentRequest("texte"), "student@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── DELETE — suppression ──────────────────────────────────────────────────

    @Nested
    class DeleteComment {

        @Test
        void author_can_delete_own_comment() {
            ReportComment comment = buildComment(1L, draftReport, trainer, Role.TRAINER, "note");

            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            service.deleteComment(1L, "trainer@test.com");

            verify(commentRepository).delete(comment);
        }

        @Test
        void admin_can_delete_any_comment() {
            ReportComment comment = buildComment(1L, draftReport, trainer, Role.TRAINER, "note");

            when(userService.requireUser("admin@test.com")).thenReturn(admin);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            service.deleteComment(1L, "admin@test.com");

            verify(commentRepository).delete(comment);
        }

        @Test
        void non_author_cannot_delete() {
            ReportComment comment = buildComment(1L, draftReport, trainer, Role.TRAINER, "note");

            when(userService.requireUser("tutor@test.com")).thenReturn(tutor);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> service.deleteComment(1L, "tutor@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void throws_when_comment_not_found() {
            when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
            when(commentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteComment(99L, "trainer@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(Long id, String email, Role role, String first, String last) {
        return User.builder().id(id).email(email).role(role)
                .firstName(first).lastName(last).password("hash").build();
    }

    private MonthlyReport buildReport(Long id, User student, ReportStatus status) {
        MonthlyReport r = new MonthlyReport();
        r.setId(id);
        r.setStudent(student);
        r.setYear(2024);
        r.setMonth(5);
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private ReportComment buildComment(Long id, MonthlyReport report, User author,
                                        Role authorRole, String content) {
        return ReportComment.builder()
                .id(id).report(report).author(author)
                .authorRole(authorRole).content(content)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
