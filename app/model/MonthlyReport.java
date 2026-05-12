package app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
    name = "monthly_reports",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_report_student_year_month",
        columnNames = {"student_id", "year", "month"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ReportSection> sections = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("changedAt ASC")
    private Set<ReportStatusLog> statusLogs = new LinkedHashSet<>();

    // ── Horodatages de validation ──────────────────────────────────────────────

    @Column(name = "student_validated_at")
    private LocalDateTime studentValidatedAt;

    @Column(name = "auto_validated_at")
    private LocalDateTime autoValidatedAt;

    @Column(name = "trainer_validated_at")
    private LocalDateTime trainerValidatedAt;

    @Column(name = "tutor_validated_at")
    private LocalDateTime tutorValidatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_student_id")
    private User validatedByStudent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_trainer_id")
    private User validatedByTrainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_tutor_id")
    private User validatedByTutor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
