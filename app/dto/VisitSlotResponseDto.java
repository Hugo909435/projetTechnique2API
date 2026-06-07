package app.dto;

import java.time.LocalDateTime;

public record VisitSlotResponseDto(
        Long id,
        LocalDateTime dateTime,
        String type,
        String typeLabel,
        boolean booked,
        Long bookedById,
        String bookedByName,
        LocalDateTime bookedAt
) {}
