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
        String schoolActivitiesPreview,
        String companyActivitiesPreview,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
