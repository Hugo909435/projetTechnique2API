package app.dto;

public record UpdateTutorRequest(
        String firstName,
        String lastName,
        String phone,
        String email
) {}
