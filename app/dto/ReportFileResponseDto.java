package app.dto;

import java.time.LocalDateTime;

public record ReportFileResponseDto(
        Long id,
        String originalFilename,
        String contentType,
        Long size,
        String uploadedBy,
        LocalDateTime createdAt
) {}
