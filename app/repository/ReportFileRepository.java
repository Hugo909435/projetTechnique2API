package app.repository;

import app.model.ReportFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportFileRepository extends JpaRepository<ReportFile, Long> {
    List<ReportFile> findByReportIdOrderByCreatedAtDesc(Long reportId);
}
