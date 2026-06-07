package app.dto;

import java.time.LocalDateTime;

public record TrainerSlotResponseDto(
        Long id,
        Long trainerId,
        String trainerName,
        LocalDateTime dateTime,
        String type,
        String typeLabel,
        String status,
        Long proposedToId,
        String proposedToName,
        Long bookedById,
        String bookedByName,
        LocalDateTime createdAt
) {}
