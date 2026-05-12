package app.repository;

import app.model.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
