package com.qldq.qldqapi.entity;

import com.qldq.qldqapi.entity.enums.GuardShiftStatus;
import com.qldq.qldqapi.entity.enums.OccurrenceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "guard_shifts",
        indexes = {
                @Index(name = "idx_guard_shifts_start_at", columnList = "start_at"),
                @Index(name = "idx_guard_shifts_status_start", columnList = "status,start_at")
        }
)
public class GuardShift extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime endAt;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuardShiftStatus status = GuardShiftStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false, foreignKey = @ForeignKey(name = "fk_guard_shifts_created_by"))
    private UserAccount createdBy;

    @OneToMany(mappedBy = "guardShift", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GuardAssignment> assignments = new ArrayList<>();

    @AssertTrue(message = "endAt must be after startAt")
    public boolean isTimeRangeValid() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }

    public OccurrenceStatus getOccurrenceStatus(Instant now) {
        if (now.isBefore(startAt.toInstant())) {
            return OccurrenceStatus.UPCOMING;
        }
        if (now.isBefore(endAt.toInstant())) {
            return OccurrenceStatus.ONGOING;
        }
        return OccurrenceStatus.COMPLETED;
    }
}
