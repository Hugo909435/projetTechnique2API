package app.dto;

public record UserResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String role
) {}
