package app.service;

import app.dto.CreateUserRequest;
import app.dto.UpdateStudentRequest;
import app.dto.UserResponseDto;
import app.exception.ConflictException;
import app.exception.ForbiddenException;
import app.exception.ResourceNotFoundException;
import app.model.Role;
import app.model.StudentProfile;
import app.model.User;
import app.repository.MonthlyReportRepository;
import app.repository.ReportCommentRepository;
import app.repository.ReportStatusLogRepository;
import app.repository.StudentProfileRepository;
import app.repository.TrainerSlotRepository;
import app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final MonthlyReportRepository monthlyReportRepository;
    private final ReportCommentRepository reportCommentRepository;
    private final ReportStatusLogRepository reportStatusLogRepository;
    private final TrainerSlotRepository trainerSlotRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       StudentProfileRepository studentProfileRepository,
                       MonthlyReportRepository monthlyReportRepository,
                       ReportCommentRepository reportCommentRepository,
                       ReportStatusLogRepository reportStatusLogRepository,
                       TrainerSlotRepository trainerSlotRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.monthlyReportRepository = monthlyReportRepository;
        this.reportCommentRepository = reportCommentRepository;
        this.reportStatusLogRepository = reportStatusLogRepository;
        this.trainerSlotRepository = trainerSlotRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsers(String requesterEmail) {
        User requester = requireUser(requesterEmail);

        return switch (requester.getRole()) {
            case ADMIN -> userRepository.findAll().stream().map(this::toDto).toList();
            case TRAINER -> {
                // Own profile + their students + those students' tutors
                List<StudentProfile> profiles = studentProfileRepository.findByTrainerId(requester.getId());
                List<UserResponseDto> result = new java.util.ArrayList<>();
                result.add(toDto(requester));
                profiles.forEach(p -> {
                    result.add(toDto(p.getStudent()));
                    if (p.getTutor() != null) result.add(toDto(p.getTutor()));
                });
                yield result.stream().distinct().toList();
            }
            default -> throw new ForbiddenException("Accès interdit");
        };
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUser(Long id, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        User target = requireUserById(id);
        assertCanReadUser(requester, target);
        return toDto(target);
    }

    // ── Création (ADMIN seulement) ────────────────────────────────────────────

    public UserResponseDto createUser(CreateUserRequest request, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        if (requester.getRole() != Role.ADMIN) throw new ForbiddenException("Seul un admin peut créer des comptes");

        if (userRepository.existsByEmail(request.email()))
            throw new ConflictException("Email déjà utilisé : " + request.email());

        Role role = parseRole(request.role());

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(role)
                .build();

        user = userRepository.save(user);

        if (role == Role.STUDENT) {
            createStudentProfile(user, request);
        }

        return toDto(user);
    }

    // ── Mise à jour ────────────────────────────────────────────────────────────

    public UserResponseDto updateUser(Long id, UpdateStudentRequest request, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        User target = requireUserById(id);
        assertCanWriteUser(requester, target);

        if (request.firstName() != null) target.setFirstName(request.firstName());
        if (request.lastName()  != null) target.setLastName(request.lastName());
        if (request.phone()     != null) target.setPhone(request.phone());

        return toDto(userRepository.save(target));
    }

    // ── Suppression (ADMIN seulement) ─────────────────────────────────────────

    public void deleteUser(Long id, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        if (requester.getRole() != Role.ADMIN) throw new ForbiddenException("Seul un admin peut supprimer des comptes");
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));

        switch (target.getRole()) {
            case STUDENT -> {
                reportCommentRepository.deleteByReportStudentId(id);
                monthlyReportRepository.deleteByStudentId(id);
                studentProfileRepository.deleteByStudentId(id);
            }
            case TRAINER -> {
                monthlyReportRepository.clearValidatedByTrainer(id);
                reportStatusLogRepository.clearChangedBy(id);
                reportCommentRepository.deleteByAuthorId(id);
                trainerSlotRepository.deleteByTrainerId(id);
                studentProfileRepository.clearTrainerFromProfiles(id);
            }
            case TUTOR -> {
                monthlyReportRepository.clearValidatedByTutor(id);
                reportStatusLogRepository.clearChangedBy(id);
                reportCommentRepository.deleteByAuthorId(id);
                trainerSlotRepository.clearProposedTo(id);
                trainerSlotRepository.clearBookedBy(id);
                studentProfileRepository.clearTutorFromProfiles(id);
            }
            default -> reportStatusLogRepository.clearChangedBy(id);
        }

        userRepository.deleteById(id);
    }

    // ── Helpers privés ─────────────────────────────────────────────────────────

    private void assertCanReadUser(User requester, User target) {
        if (requester.getRole() == Role.ADMIN) return;
        if (requester.getId().equals(target.getId())) return;

        if (requester.getRole() == Role.TRAINER) {
            if (studentProfileRepository.existsByStudentIdAndTrainerId(target.getId(), requester.getId())) return;
            if (studentProfileRepository.existsByTutorIdAndTrainerId(target.getId(), requester.getId())) return;
        }

        throw new ForbiddenException("Accès interdit");
    }

    private void assertCanWriteUser(User requester, User target) {
        if (requester.getRole() == Role.ADMIN) return;
        if (requester.getId().equals(target.getId())) return;

        if (requester.getRole() == Role.TRAINER) {
            if (studentProfileRepository.existsByStudentIdAndTrainerId(target.getId(), requester.getId())) return;
            if (studentProfileRepository.existsByTutorIdAndTrainerId(target.getId(), requester.getId())) return;
        }

        throw new ForbiddenException("Accès interdit");
    }

    private void createStudentProfile(User student, CreateUserRequest request) {
        StudentProfile.StudentProfileBuilder builder = StudentProfile.builder()
                .student(student)
                .studentNumber(request.studentNumber())
                .companyName(request.companyName());

        if (request.trainerId() != null) {
            builder.trainer(requireUserById(request.trainerId()));
        }
        if (request.tutorId() != null) {
            builder.tutor(requireUserById(request.tutorId()));
        }

        studentProfileRepository.save(builder.build());
    }

    private Role parseRole(String roleStr) {
        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rôle invalide : " + roleStr);
        }
    }

    public User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
    }

    private User requireUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }

    public UserResponseDto toDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name()
        );
    }
}
