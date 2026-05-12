package app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "report_sections",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_section_report_type",
        columnNames = {"report_id", "section_type"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private MonthlyReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false)
    private ReportSectionType sectionType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
