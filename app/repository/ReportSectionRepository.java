package app.repository;

import app.model.ReportSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportSectionRepository extends JpaRepository<ReportSection, Long> {
    List<ReportSection> findByReportId(Long reportId);
}
