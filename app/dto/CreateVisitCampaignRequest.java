package app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateVisitCampaignRequest(
        @NotEmpty(message = "Au moins un créneau est requis")
        List<@Valid SlotRequest> slots
) {
    public record SlotRequest(
            @NotNull(message = "La date/heure est requise")
            LocalDateTime dateTime,
            @NotNull(message = "Le type est requis")
            String type
    ) {}
}
