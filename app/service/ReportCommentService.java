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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReportCommentService {

    private final ReportCommentRepository commentRepository;
    private final MonthlyReportRepository reportRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserService userService;

    public ReportCommentService(ReportCommentRepository commentRepository,
                                MonthlyReportRepository reportRepository,
                                StudentProfileRepository studentProfileRepository,
                                UserService userService) {
        this.commentRepository = commentRepository;
        this.reportRepository = reportRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userService = userService;
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReportCommentResponseDto> getComments(Long reportId, String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        assertNotStudent(requester);

        MonthlyReport report = requireReport(reportId);
        assertCanAccessReport(report, requester);

        return commentRepository.findByReportIdOrderByCreatedAtAsc(reportId)
                .stream()
                .filter(c -> isVisible(c, report, requester))
                .map(c -> toDto(c, requester))
                .toList();
    }

    // ── Création ──────────────────────────────────────────────────────────────

    public ReportCommentResponseDto createComment(Long reportId,
                                                   CreateReportCommentRequest req,
                                                   String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        assertCanWrite(requester);

        MonthlyReport report = requireReport(reportId);
        assertCanAccessReport(report, requester);

        ReportComment saved = commentRepository.save(ReportComment.builder()
                .report(report)
                .author(requester)
                .authorRole(requester.getRole())
                .content(req.content())
                .build());

        return toDto(saved, requester);
    }

    // ── Modification ──────────────────────────────────────────────────────────

    public ReportCommentResponseDto updateComment(Long commentId,
                                                   UpdateReportCommentRequest req,
                                                   String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        assertNotStudent(requester);

        ReportComment comment = requireComment(commentId);

        if (!comment.getAuthor().getId().equals(requester.getId()))
            throw new ForbiddenException("Vous ne pouvez modifier que vos propres commentaires");

        comment.setContent(req.content());
        return toDto(commentRepository.save(comment), requester);
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    public void deleteComment(Long commentId, String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        assertNotStudent(requester);

        ReportComment comment = requireComment(commentId);

        boolean isAuthor = comment.getAuthor().getId().equals(requester.getId());
        boolean isAdmin  = requester.getRole() == Role.ADMIN;

        if (!isAuthor && !isAdmin)
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres commentaires");

        commentRepository.delete(comment);
    }

    // ── Logique de visibilité ─────────────────────────────────────────────────

    /**
     * Règle de visibilité :
     *  - ADMIN           → voit tout
     *  - TRAINER         → ses propres commentaires toujours ; ceux du tuteur seulement après COMPLETED
     *  - TUTOR           → ses propres commentaires toujours ; ceux du formateur seulement après COMPLETED
     */
    private boolean isVisible(ReportComment comment, MonthlyReport report, User requester) {
        return switch (requester.getRole()) {
            case ADMIN -> true;
            case TRAINER -> comment.getAuthor().getId().equals(requester.getId())
                    || (comment.getAuthorRole() == Role.TUTOR
                    && report.getStatus() == ReportStatus.COMPLETED);
            case TUTOR -> comment.getAuthor().getId().equals(requester.getId())
                    || (comment.getAuthorRole() == Role.TRAINER
                    && report.getStatus() == ReportStatus.COMPLETED);
            default -> false;
        };
    }

    // ── Contrôle d'accès ──────────────────────────────────────────────────────

    private void assertNotStudent(User user) {
        if (user.getRole() == Role.STUDENT)
            throw new ForbiddenException("Les étudiants n'ont pas accès aux commentaires internes");
    }

    /** TRAINER et TUTOR uniquement (pas ADMIN, pas STUDENT). */
    private void assertCanWrite(User user) {
        if (user.getRole() != Role.TRAINER && user.getRole() != Role.TUTOR)
            throw new ForbiddenException(
                    "Seuls le formateur et le tuteur peuvent écrire des commentaires internes");
    }

    /** Vérifie que le demandeur a accès au rapport (ownership par rôle). */
    private void assertCanAccessReport(MonthlyReport report, User requester) {
        switch (requester.getRole()) {
            case ADMIN -> {}
            case TRAINER -> {
                if (!studentProfileRepository.existsByStudentIdAndTrainerId(
                        report.getStudent().getId(), requester.getId()))
                    throw new ForbiddenException("Ce rapport n'appartient pas à l'un de vos étudiants");
            }
            case TUTOR -> {
                if (!studentProfileRepository.existsByStudentIdAndTutorId(
                        report.getStudent().getId(), requester.getId()))
                    throw new ForbiddenException("Ce rapport n'appartient pas à l'un de vos étudiants");
            }
            default -> throw new ForbiddenException("Accès interdit");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MonthlyReport requireReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable : " + id));
    }

    private ReportComment requireComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire introuvable : " + id));
    }

    private ReportCommentResponseDto toDto(ReportComment c, User requester) {
        boolean canEdit   = c.getAuthor().getId().equals(requester.getId());
        boolean canDelete = canEdit || requester.getRole() == Role.ADMIN;
        return new ReportCommentResponseDto(
                c.getId(),
                c.getReport().getId(),
                c.getAuthor().getId(),
                c.getAuthor().getFirstName() + " " + c.getAuthor().getLastName(),
                c.getAuthorRole().name(),
                c.getContent(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                canEdit,
                canDelete);
    }
}
