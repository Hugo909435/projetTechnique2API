package app.controller;

import app.dto.CreateReportCommentRequest;
import app.dto.ReportCommentResponseDto;
import app.dto.UpdateReportCommentRequest;
import app.service.ReportCommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReportCommentController {

    private final ReportCommentService commentService;

    public ReportCommentController(ReportCommentService commentService) {
        this.commentService = commentService;
    }

    /** Commentaires visibles par le demandeur sur ce rapport. STUDENT → 403. */
    @GetMapping("/api/reports/{reportId}/comments")
    public ResponseEntity<List<ReportCommentResponseDto>> getComments(
            @PathVariable Long reportId, Authentication auth) {
        return ResponseEntity.ok(commentService.getComments(reportId, auth.getName()));
    }

    /** Ajoute un commentaire — TRAINER ou TUTOR sur ses propres étudiants uniquement. */
    @PostMapping("/api/reports/{reportId}/comments")
    public ResponseEntity<ReportCommentResponseDto> createComment(
            @PathVariable Long reportId,
            @Valid @RequestBody CreateReportCommentRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(reportId, request, auth.getName()));
    }

    /** Modifie un commentaire — auteur uniquement. */
    @PutMapping("/api/report-comments/{commentId}")
    public ResponseEntity<ReportCommentResponseDto> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateReportCommentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(commentService.updateComment(commentId, request, auth.getName()));
    }

    /** Supprime un commentaire — auteur ou ADMIN. */
    @DeleteMapping("/api/report-comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId, Authentication auth) {
        commentService.deleteComment(commentId, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
