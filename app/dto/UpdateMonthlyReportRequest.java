package app.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateMonthlyReportRequest(
        @NotNull List<SectionUpdate> sections
) {
    public record SectionUpdate(
            @NotNull String sectionType,
            String content
    ) {}
}
