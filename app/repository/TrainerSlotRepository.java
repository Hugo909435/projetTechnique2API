package app.repository;

import app.model.TrainerSlot;
import app.model.TrainerSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrainerSlotRepository extends JpaRepository<TrainerSlot, Long> {

    List<TrainerSlot> findByTrainerIdOrderByDateTimeAsc(Long trainerId);

    /** Créneaux proposés OU confirmés pour un tuteur (sa vue complète). */
    @Query("SELECT s FROM TrainerSlot s WHERE s.proposedTo.id = :tutorId OR s.bookedBy.id = :tutorId ORDER BY s.dateTime ASC")
    List<TrainerSlot> findByTutorOrderByDateTimeAsc(@Param("tutorId") Long tutorId);

    List<TrainerSlot> findByTrainerIdAndProposedToIdAndStatus(
            Long trainerId, Long tutorId, TrainerSlotStatus status);

    boolean existsByTrainerIdAndDateTimeBetweenAndStatusIn(
            Long trainerId, LocalDateTime from, LocalDateTime to, List<TrainerSlotStatus> statuses);

    @Query("SELECT s FROM TrainerSlot s WHERE s.trainer.id = :trainerId AND s.proposedTo.id = :tutorId AND s.status = 'PROPOSED'")
    List<TrainerSlot> findProposedByTrainerToTutor(@Param("trainerId") Long trainerId,
                                                    @Param("tutorId") Long tutorId);
}
