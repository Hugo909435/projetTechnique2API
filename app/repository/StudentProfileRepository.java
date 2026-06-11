package app.repository;

import app.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByStudentId(Long studentId);

    Optional<StudentProfile> findByStudentEmail(String email);

    List<StudentProfile> findByTrainerId(Long trainerId);

    List<StudentProfile> findByTutorId(Long tutorId);

    boolean existsByStudentIdAndTrainerId(Long studentId, Long trainerId);

    boolean existsByStudentIdAndTutorId(Long studentId, Long tutorId);

    @Query("SELECT CASE WHEN COUNT(sp) > 0 THEN true ELSE false END " +
           "FROM StudentProfile sp WHERE sp.tutor.id = :tutorId AND sp.trainer.id = :trainerId")
    boolean existsByTutorIdAndTrainerId(@Param("tutorId") Long tutorId, @Param("trainerId") Long trainerId);

    void deleteByStudentId(Long studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE StudentProfile sp SET sp.trainer = null WHERE sp.trainer.id = :trainerId")
    void clearTrainerFromProfiles(@Param("trainerId") Long trainerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE StudentProfile sp SET sp.tutor = null WHERE sp.tutor.id = :tutorId")
    void clearTutorFromProfiles(@Param("tutorId") Long tutorId);
}
