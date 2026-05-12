package app.dto;

import jakarta.validation.constraints.Size;

public record ValidateReportRequest(
        @Size(max = 5000, message = "Commentaire trop long (max 5000 caractères)")
        String comment
) {}
