package app.dto;

public record StudentResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String studentNumber,
        String companyName,
        UserResponseDto trainer,
        UserResponseDto tutor
) {}
