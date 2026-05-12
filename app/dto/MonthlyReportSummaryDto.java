package app.dto;

import java.time.LocalDateTime;

public record MonthlyReportSummaryDto(
        Long id,
        int year,
        int month,
        String monthLabel,
        String status,
        Long studentId,
        String studentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
