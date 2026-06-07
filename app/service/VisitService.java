package app.service;

import app.dto.CreateTrainerSlotRequest;
import app.dto.ProposeSlotRequest;
import app.dto.TrainerSlotResponseDto;
import app.dto.TutorVisitStatusDto;
import app.exception.ConflictException;
import app.exception.ForbiddenException;
import app.exception.ResourceNotFoundException;
import app.model.*;
import app.repository.StudentProfileRepository;
import app.repository.TrainerSlotRepository;
import app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class VisitService {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    private final TrainerSlotRepository slotRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final EmailService emailService;

    public VisitService(TrainerSlotRepository slotRepository,
                        UserRepository userRepository,
                        StudentProfileRepository studentProfileRepository,
                        EmailService emailService) {
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.emailService = emailService;
    }

    // ── Formateur : créer un créneau ────────────────────────────────────────────

    @Transactional
    public TrainerSlotResponseDto createSlot(CreateTrainerSlotRequest req, String trainerEmail) {
        User trainer = getUser(trainerEmail);
        requireRole(trainer, Role.TRAINER);

        // Bloquer si un créneau existe déjà ±30 min sur ce même créneau
        LocalDateTime from = req.dateTime().minusMinutes(29);
        LocalDateTime to   = req.dateTime().plusMinutes(29);
        if (slotRepository.existsByTrainerIdAndDateTimeBetweenAndStatusIn(
                trainer.getId(), from, to,
                List.of(TrainerSlotStatus.FREE, TrainerSlotStatus.PROPOSED, TrainerSlotStatus.BOOKED))) {
            throw new ConflictException("Un créneau existe déjà à cette heure");
        }

        TrainerSlot slot = TrainerSlot.builder()
                .trainer(trainer)
                .dateTime(req.dateTime())
                .type(VisitType.valueOf(req.type()))
                .build();

        return toDto(slotRepository.save(slot));
    }

    // ── Formateur : lister ses créneaux / Tuteur : voir ses créneaux ─────────────

    @Transactional(readOnly = true)
    public List<TrainerSlotResponseDto> getSlots(String userEmail) {
        User user = getUser(userEmail);
        return switch (user.getRole()) {
            case TRAINER -> slotRepository.findByTrainerIdOrderByDateTimeAsc(user.getId())
                    .stream().map(this::toDto).toList();
            case TUTOR   -> slotRepository.findByTutorOrderByDateTimeAsc(user.getId())
                    .stream().map(this::toDto).toList();
            default      -> throw new ForbiddenException("Accès non autorisé");
        };
    }

    // ── Formateur : liste des tuteurs avec statut de visite ─────────────────────

    @Transactional(readOnly = true)
    public List<TutorVisitStatusDto> getTutors(String trainerEmail) {
        User trainer = getUser(trainerEmail);
        requireRole(trainer, Role.TRAINER);

        return studentProfileRepository.findByTrainerId(trainer.getId()).stream()
                .filter(p -> p.getTutor() != null)
                .map(p -> p.getTutor())
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u, (a, b) -> a))
                .values().stream()
                .map(tutor -> buildTutorStatus(trainer.getId(), tutor))
                .sorted(java.util.Comparator.comparing(TutorVisitStatusDto::lastName))
                .toList();
    }

    // ── Formateur : proposer un créneau à un tuteur ──────────────────────────────

    @Transactional
    public TrainerSlotResponseDto proposeSlot(Long slotId, ProposeSlotRequest req, String trainerEmail) {
        User trainer = getUser(trainerEmail);
        requireRole(trainer, Role.TRAINER);

        TrainerSlot slot = getSlot(slotId);
        if (!slot.getTrainer().getId().equals(trainer.getId())) throw new ForbiddenException("Accès non autorisé");
        if (slot.getStatus() != TrainerSlotStatus.FREE) throw new ConflictException("Ce créneau n'est plus disponible");

        User tutor = userRepository.findById(req.tutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Tuteur introuvable"));
        if (tutor.getRole() != Role.TUTOR) throw new ForbiddenException("Cet utilisateur n'est pas un tuteur");

        if (!studentProfileRepository.existsByTutorIdAndTrainerId(tutor.getId(), trainer.getId())) {
            throw new ForbiddenException("Ce tuteur n'est pas lié à vos étudiants");
        }

        slot.setStatus(TrainerSlotStatus.PROPOSED);
        slot.setProposedTo(tutor);
        slot.setProposedAt(LocalDateTime.now());

        TrainerSlotResponseDto result = toDto(slotRepository.save(slot));

        try {
            emailService.sendVisitNotification(tutor, trainer);
        } catch (Exception ignored) {
            // L'échec de l'email ne doit pas annuler la proposition
        }

        return result;
    }

    // ── Formateur : annuler une proposition ─────────────────────────────────────

    @Transactional
    public TrainerSlotResponseDto cancelProposal(Long slotId, String trainerEmail) {
        User trainer = getUser(trainerEmail);
        requireRole(trainer, Role.TRAINER);

        TrainerSlot slot = getSlot(slotId);
        if (!slot.getTrainer().getId().equals(trainer.getId())) throw new ForbiddenException("Accès non autorisé");
        if (slot.getStatus() != TrainerSlotStatus.PROPOSED) throw new ConflictException("Ce créneau n'est pas en attente");

        slot.setStatus(TrainerSlotStatus.FREE);
        slot.setProposedTo(null);
        slot.setProposedAt(null);

        return toDto(slotRepository.save(slot));
    }

    // ── Formateur : supprimer un créneau libre ───────────────────────────────────

    @Transactional
    public void deleteSlot(Long slotId, String trainerEmail) {
        User trainer = getUser(trainerEmail);
        requireRole(trainer, Role.TRAINER);

        TrainerSlot slot = getSlot(slotId);
        if (!slot.getTrainer().getId().equals(trainer.getId())) throw new ForbiddenException("Accès non autorisé");
        if (slot.getStatus() == TrainerSlotStatus.PROPOSED || slot.getStatus() == TrainerSlotStatus.BOOKED)
            throw new ConflictException("Impossible de supprimer un créneau proposé ou confirmé");

        slotRepository.delete(slot);
    }

    // ── Tuteur : confirmer un créneau ────────────────────────────────────────────

    @Transactional
    public TrainerSlotResponseDto confirmSlot(Long slotId, String tutorEmail) {
        User tutor = getUser(tutorEmail);
        requireRole(tutor, Role.TUTOR);

        TrainerSlot slot = getSlot(slotId);
        if (slot.getStatus() != TrainerSlotStatus.PROPOSED) throw new ConflictException("Ce créneau n'est pas proposé");
        if (!slot.getProposedTo().getId().equals(tutor.getId())) throw new ForbiddenException("Ce créneau n'est pas proposé à vous");

        // Les autres créneaux proposés à ce tuteur par ce formateur → CANCELLED
        slotRepository.findProposedByTrainerToTutor(slot.getTrainer().getId(), tutor.getId()).stream()
                .filter(s -> !s.getId().equals(slotId))
                .forEach(s -> {
                    s.setStatus(TrainerSlotStatus.CANCELLED);
                    slotRepository.save(s);
                });

        slot.setStatus(TrainerSlotStatus.BOOKED);
        slot.setBookedBy(tutor);
        slot.setBookedAt(LocalDateTime.now());

        return toDto(slotRepository.save(slot));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private TutorVisitStatusDto buildTutorStatus(Long trainerId, User tutor) {
        List<TrainerSlot> proposed = slotRepository.findProposedByTrainerToTutor(trainerId, tutor.getId());

        List<TrainerSlot> booked = slotRepository
                .findByTrainerIdAndProposedToIdAndStatus(trainerId, tutor.getId(), TrainerSlotStatus.BOOKED);

        String status;
        Long confirmedSlotId = null;
        String confirmedSlotDate = null;

        if (!booked.isEmpty()) {
            status = "CONFIRMED";
            TrainerSlot b = booked.get(0);
            confirmedSlotId = b.getId();
            confirmedSlotDate = b.getDateTime().format(DISPLAY_FMT);
        } else if (!proposed.isEmpty()) {
            status = "PENDING";
        } else {
            status = "NONE";
        }

        StudentProfile profile = studentProfileRepository.findByTutorId(tutor.getId())
                .stream().findFirst().orElse(null);

        return new TutorVisitStatusDto(
                tutor.getId(),
                tutor.getFirstName(),
                tutor.getLastName(),
                profile != null ? profile.getCompanyName() : null,
                status,
                confirmedSlotId,
                confirmedSlotDate
        );
    }

    private TrainerSlotResponseDto toDto(TrainerSlot s) {
        return new TrainerSlotResponseDto(
                s.getId(),
                s.getTrainer().getId(),
                s.getTrainer().getFirstName() + " " + s.getTrainer().getLastName(),
                s.getDateTime(),
                s.getType().name(),
                s.getType().label(),
                s.getStatus().name(),
                s.getProposedTo() != null ? s.getProposedTo().getId() : null,
                s.getProposedTo() != null
                        ? s.getProposedTo().getFirstName() + " " + s.getProposedTo().getLastName()
                        : null,
                s.getBookedBy() != null ? s.getBookedBy().getId() : null,
                s.getBookedBy() != null
                        ? s.getBookedBy().getFirstName() + " " + s.getBookedBy().getLastName()
                        : null,
                s.getCreatedAt()
        );
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private TrainerSlot getSlot(Long id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Créneau introuvable"));
    }

    private void requireRole(User user, Role role) {
        if (user.getRole() != role) throw new ForbiddenException("Accès non autorisé");
    }
}
