package app.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VisitCampaignResponseDto(
        Long id,
        Long trainerId,
        String trainerName,
        LocalDateTime createdAt,
        int totalSlots,
        int bookedSlots,
        List<VisitSlotResponseDto> slots
) {}
