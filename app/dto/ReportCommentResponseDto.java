package app.dto;

import java.time.LocalDateTime;

public record ReportCommentResponseDto(
        Long id,
        Long reportId,
        Long authorId,
        String authorName,
        String authorRole,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean canEdit,
        boolean canDelete
) {}
