package app.dto;

public record ReportSectionDto(
        Long id,
        String sectionType,
        String label,
        String content
) {}
