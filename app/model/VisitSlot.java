package app.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "visit_slots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private VisitCampaign campaign;

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitType type;

    @Builder.Default
    @Column(nullable = false)
    private boolean booked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by_id")
    private User bookedBy;

    @Column(name = "booked_at")
    private LocalDateTime bookedAt;
}
