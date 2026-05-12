package app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReportCommentRequest(
        @NotBlank(message = "Le contenu ne peut pas être vide")
        @Size(max = 5000, message = "Commentaire trop long (max 5000 caractères)")
        String content
) {}
