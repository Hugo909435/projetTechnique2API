package app.dto;

import java.time.LocalDateTime;

public record ReportStatusLogDto(
        String fromStatus,
        String toStatus,
        String changedByName,
        LocalDateTime changedAt,
        String note
) {}
