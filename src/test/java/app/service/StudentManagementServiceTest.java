package app.service;

import app.dto.StudentResponseDto;
import app.dto.UpdateStudentRequest;
import app.dto.UserResponseDto;
import app.exception.ForbiddenException;
import app.model.Role;
import app.model.StudentProfile;
import app.model.User;
import app.repository.StudentProfileRepository;
import app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentManagementServiceTest {

    @Mock StudentProfileRepository studentProfileRepository;
    @Mock UserRepository userRepository;
    @Mock UserService userService;

    @InjectMocks StudentManagementService service;

    private User trainer;
    private User otherTrainer;
    private User tutor;
    private User student;
    private StudentProfile profile;

    private UserResponseDto trainerDto;
    private UserResponseDto tutorDto;

    @BeforeEach
    void setUp() {
        trainer = buildUser(1L, "trainer@test.com", Role.TRAINER, "Jean", "Dupont");
        otherTrainer = buildUser(99L, "other@test.com", Role.TRAINER, "Paul", "Autre");
        tutor   = buildUser(2L, "tutor@test.com",   Role.TUTOR,    "Pierre", "Martin");
        student = buildUser(3L, "student@test.com", Role.STUDENT,  "Alice",  "Durand");

        profile = StudentProfile.builder().id(1L).student(student)
                .trainer(trainer).tutor(tutor)
                .studentNumber("ALT-001").companyName("TechCorp").build();

        trainerDto = new UserResponseDto(1L, "trainer@test.com", "Jean", "Dupont", null, "TRAINER");
        tutorDto   = new UserResponseDto(2L, "tutor@test.com",   "Pierre", "Martin", null, "TUTOR");
    }

    @Test
    void trainer_receives_only_own_students() {
        when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
        when(studentProfileRepository.findByTrainerId(1L)).thenReturn(List.of(profile));
        when(userService.toDto(trainer)).thenReturn(trainerDto);
        when(userService.toDto(tutor)).thenReturn(tutorDto);

        List<StudentResponseDto> result = service.getStudents("trainer@test.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("student@test.com");
    }

    @Test
    void trainer_cannot_access_student_of_another_trainer() {
        StudentProfile otherProfile = StudentProfile.builder().id(2L)
                .student(student).trainer(otherTrainer).build();

        when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
        when(studentProfileRepository.findByStudentId(3L)).thenReturn(Optional.of(otherProfile));

        assertThatThrownBy(() -> service.getStudent(3L, "trainer@test.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("interdit");
    }

    @Test
    void trainer_can_update_own_student_profile() {
        when(userService.requireUser("trainer@test.com")).thenReturn(trainer);
        when(studentProfileRepository.findByStudentId(3L)).thenReturn(Optional.of(profile));
        when(userRepository.save(student)).thenReturn(student);
        when(studentProfileRepository.save(profile)).thenReturn(profile);
        when(userService.toDto(trainer)).thenReturn(trainerDto);
        when(userService.toDto(tutor)).thenReturn(tutorDto);

        var result = service.updateStudent(3L,
                new UpdateStudentRequest("Alicia", null, null, "ALT-002", null),
                "trainer@test.com");

        assertThat(result.firstName()).isEqualTo("Alicia");
        assertThat(result.studentNumber()).isEqualTo("ALT-002");
    }

    @Test
    void student_can_read_own_profile() {
        when(userService.requireUser("student@test.com")).thenReturn(student);
        when(studentProfileRepository.findByStudentId(3L)).thenReturn(Optional.of(profile));
        when(userService.toDto(trainer)).thenReturn(trainerDto);
        when(userService.toDto(tutor)).thenReturn(tutorDto);

        var result = service.getStudent(3L, "student@test.com");

        assertThat(result.email()).isEqualTo("student@test.com");
        assertThat(result.companyName()).isEqualTo("TechCorp");
    }

    @Test
    void student_cannot_update_own_profile() {
        when(userService.requireUser("student@test.com")).thenReturn(student);
        when(studentProfileRepository.findByStudentId(3L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.updateStudent(3L,
                new UpdateStudentRequest("Hack", null, null, null, null),
                "student@test.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildUser(Long id, String email, Role role, String first, String last) {
        return User.builder().id(id).email(email).role(role)
                .firstName(first).lastName(last).password("hash").build();
    }
}
