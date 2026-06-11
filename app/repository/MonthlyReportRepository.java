package app.repository;

import app.model.MonthlyReport;
import app.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {

    boolean existsByStudentIdAndYearAndMonth(Long studentId, int year, int month);

    List<MonthlyReport> findByStatusAndYearAndMonth(ReportStatus status, int year, int month);

    List<MonthlyReport> findByStudentIdInAndStatusInOrderByYearDescMonthDesc(
            List<Long> studentIds, List<ReportStatus> statuses);

    Optional<MonthlyReport> findByStudentIdAndYearAndMonth(Long studentId, int year, int month);

    List<MonthlyReport> findByStudentIdOrderByYearDescMonthDesc(Long studentId);

    List<MonthlyReport> findByStudentIdInOrderByYearDescMonthDesc(List<Long> studentIds);

    /** Charge le rapport avec ses sections via JOIN FETCH. Les statusLogs sont chargés lazily dans la transaction. */
    @Query("SELECT DISTINCT r FROM MonthlyReport r "
         + "LEFT JOIN FETCH r.sections "
         + "WHERE r.id = :id")
    Optional<MonthlyReport> findByIdWithSections(@Param("id") Long id);

    void deleteByStudentId(Long studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MonthlyReport r SET r.validatedByTrainer = null WHERE r.validatedByTrainer.id = :trainerId")
    void clearValidatedByTrainer(@Param("trainerId") Long trainerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MonthlyReport r SET r.validatedByTutor = null WHERE r.validatedByTutor.id = :tutorId")
    void clearValidatedByTutor(@Param("tutorId") Long tutorId);
}
