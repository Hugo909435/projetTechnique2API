package app.dto;

public record AuthResponse(String token, String type, UserDto user) {

    public record UserDto(
            Long id,
            String email,
            String firstName,
            String lastName,
            String role
    ) {}
}
