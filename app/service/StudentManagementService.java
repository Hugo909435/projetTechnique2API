package app.service;

import app.dto.StudentResponseDto;
import app.dto.UpdateStudentRequest;
import app.dto.UpdateTutorRequest;
import app.dto.UserResponseDto;
import app.exception.ForbiddenException;
import app.exception.ResourceNotFoundException;
import app.model.Role;
import app.model.StudentProfile;
import app.model.User;
import app.repository.StudentProfileRepository;
import app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StudentManagementService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public StudentManagementService(StudentProfileRepository studentProfileRepository,
                                    UserRepository userRepository,
                                    UserService userService) {
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StudentResponseDto> getStudents(String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);

        List<StudentProfile> profiles = switch (requester.getRole()) {
            case ADMIN   -> studentProfileRepository.findAll();
            case TRAINER -> studentProfileRepository.findByTrainerId(requester.getId());
            case TUTOR   -> studentProfileRepository.findByTutorId(requester.getId());
            case STUDENT -> {
                StudentProfile own = studentProfileRepository.findByStudentId(requester.getId())
                        .orElse(null);
                yield own != null ? List.of(own) : List.of();
            }
        };

        return profiles.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponseDto getStudent(Long studentId, String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        StudentProfile profile = requireProfileByStudentId(studentId);
        assertCanAccessStudent(requester, profile);
        return toDto(profile);
    }

    // ── Mise à jour du profil étudiant (TRAINER sur ses étudiants, ADMIN partout) ──

    public StudentResponseDto updateStudent(Long studentId, UpdateStudentRequest request, String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        StudentProfile profile = requireProfileByStudentId(studentId);
        assertCanManageStudent(requester, profile);

        User student = profile.getStudent();
        if (request.firstName()    != null) student.setFirstName(request.firstName());
        if (request.lastName()     != null) student.setLastName(request.lastName());
        if (request.phone()        != null) student.setPhone(request.phone());
        if (request.studentNumber() != null) profile.setStudentNumber(request.studentNumber());
        if (request.companyName()  != null) profile.setCompanyName(request.companyName());

        userRepository.save(student);
        return toDto(studentProfileRepository.save(profile));
    }

    // ── Mise à jour du tuteur associé à un étudiant (TRAINER sur ses étudiants, ADMIN) ──

    public UserResponseDto updateTutor(Long studentId, UpdateTutorRequest request, String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        StudentProfile profile = requireProfileByStudentId(studentId);
        assertCanManageStudent(requester, profile);

        User tutor = profile.getTutor();
        if (tutor == null)
            throw new ResourceNotFoundException("Cet étudiant n'a pas de tuteur assigné");

        if (request.firstName() != null) tutor.setFirstName(request.firstName());
        if (request.lastName()  != null) tutor.setLastName(request.lastName());
        if (request.phone()     != null) tutor.setPhone(request.phone());
        if (request.email()     != null) tutor.setEmail(request.email());

        return userService.toDto(userRepository.save(tutor));
    }

    // ── Assignation (ADMIN seulement) ──────────────────────────────────────────

    public StudentResponseDto assignTrainer(Long studentId, Long trainerId, String requesterEmail) {
        assertIsAdmin(requesterEmail);
        StudentProfile profile = requireProfileByStudentId(studentId);
        User trainer = requireUserWithRole(trainerId, Role.TRAINER);
        profile.setTrainer(trainer);
        return toDto(studentProfileRepository.save(profile));
    }

    public StudentResponseDto assignTutor(Long studentId, Long tutorId, String requesterEmail) {
        assertIsAdmin(requesterEmail);
        StudentProfile profile = requireProfileByStudentId(studentId);
        User tutor = requireUserWithRole(tutorId, Role.TUTOR);
        profile.setTutor(tutor);
        return toDto(studentProfileRepository.save(profile));
    }

    // ── Helpers privés ─────────────────────────────────────────────────────────

    private void assertCanAccessStudent(User requester, StudentProfile profile) {
        if (requester.getRole() == Role.ADMIN) return;
        if (requester.getRole() == Role.TRAINER
                && profile.getTrainer() != null
                && profile.getTrainer().getId().equals(requester.getId())) return;
        if (requester.getRole() == Role.TUTOR
                && profile.getTutor() != null
                && profile.getTutor().getId().equals(requester.getId())) return;
        if (requester.getRole() == Role.STUDENT
                && profile.getStudent().getId().equals(requester.getId())) return;
        throw new ForbiddenException("Accès interdit à cet étudiant");
    }

    private void assertCanManageStudent(User requester, StudentProfile profile) {
        if (requester.getRole() == Role.ADMIN) return;
        if (requester.getRole() == Role.TRAINER
                && profile.getTrainer() != null
                && profile.getTrainer().getId().equals(requester.getId())) return;
        throw new ForbiddenException("Vous n'êtes pas le formateur de cet étudiant");
    }

    private void assertIsAdmin(String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        if (requester.getRole() != Role.ADMIN)
            throw new ForbiddenException("Seul un admin peut effectuer cette action");
    }

    private StudentProfile requireProfileByStudentId(Long studentId) {
        return studentProfileRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil étudiant introuvable pour l'id : " + studentId));
    }

    private User requireUserWithRole(Long userId, Role expectedRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + userId));
        if (user.getRole() != expectedRole)
            throw new RuntimeException("L'utilisateur " + userId + " n'a pas le rôle " + expectedRole);
        return user;
    }

    public StudentResponseDto toDto(StudentProfile profile) {
        return new StudentResponseDto(
                profile.getStudent().getId(),
                profile.getStudent().getEmail(),
                profile.getStudent().getFirstName(),
                profile.getStudent().getLastName(),
                profile.getStudent().getPhone(),
                profile.getStudentNumber(),
                profile.getCompanyName(),
                profile.getTrainer() != null ? userService.toDto(profile.getTrainer()) : null,
                profile.getTutor()   != null ? userService.toDto(profile.getTutor())   : null
        );
    }
}
