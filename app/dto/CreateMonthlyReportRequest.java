package app.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateMonthlyReportRequest(
        @NotNull @Min(2020) Integer year,
        @NotNull @Min(1) @Max(12) Integer month
) {}
