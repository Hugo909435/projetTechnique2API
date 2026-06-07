package app.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateTrainerSlotRequest(
        @NotNull(message = "La date/heure est requise")
        LocalDateTime dateTime,
        @NotNull(message = "Le type est requis")
        String type
) {}
