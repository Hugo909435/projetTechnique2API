package app.dto;

public record TutorVisitStatusDto(
        Long id,
        String firstName,
        String lastName,
        String companyName,
        String visitStatus,       // NONE, PENDING, CONFIRMED
        Long confirmedSlotId,
        String confirmedSlotDate
) {}
