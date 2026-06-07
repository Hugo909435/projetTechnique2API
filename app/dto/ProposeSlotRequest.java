package app.dto;

import jakarta.validation.constraints.NotNull;

public record ProposeSlotRequest(
        @NotNull(message = "L'identifiant du tuteur est requis")
        Long tutorId
) {}
