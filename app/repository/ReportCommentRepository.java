package app.repository;

import app.model.ReportComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {
    List<ReportComment> findByReportIdOrderByCreatedAtAsc(Long reportId);

    void deleteByAuthorId(Long authorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ReportComment c WHERE c.report.id IN (SELECT r.id FROM MonthlyReport r WHERE r.student.id = :studentId)")
    void deleteByReportStudentId(@Param("studentId") Long studentId);
}
