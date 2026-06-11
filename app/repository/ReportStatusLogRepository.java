package app.repository;

import app.model.ReportStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportStatusLogRepository extends JpaRepository<ReportStatusLog, Long> {
    List<ReportStatusLog> findByReportIdOrderByChangedAtAsc(Long reportId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ReportStatusLog l SET l.changedBy = null WHERE l.changedBy.id = :userId")
    void clearChangedBy(@Param("userId") Long userId);
}
