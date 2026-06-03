package app.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MonthlyReportResponseDto(
        Long id,
        int year,
        int month,
        String monthLabel,
        String status,
        boolean editable,
        boolean studentCanEditAndReset,
        Long studentId,
        String studentName,
        List<ReportSectionDto> sections,
        List<ReportStatusLogDto> statusLogs,
        LocalDateTime studentValidatedAt,
        LocalDateTime autoValidatedAt,
        LocalDateTime trainerValidatedAt,
        LocalDateTime tutorValidatedAt,
        LocalDateTime completedAt,
        String trainerNote,
        String tutorNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
