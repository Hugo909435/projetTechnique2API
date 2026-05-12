package app.dto;

public record UpdateStudentRequest(
        String firstName,
        String lastName,
        String phone,
        String studentNumber,
        String companyName
) {}
