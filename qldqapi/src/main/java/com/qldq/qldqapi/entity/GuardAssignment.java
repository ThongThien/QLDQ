package com.qldq.qldqapi.entity;

import com.qldq.qldqapi.entity.enums.GuardAssignmentStatus;
import com.qldq.qldqapi.entity.enums.GuardResultCondition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "guard_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_guard_assignments_shift_soldier",
                columnNames = {"guard_shift_id", "soldier_id"}
        ),
        indexes = {
                @Index(name = "idx_guard_assignments_soldier", columnList = "soldier_id"),
                @Index(name = "idx_guard_assignments_shift_status", columnList = "guard_shift_id,status")
        }
)
public class GuardAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guard_shift_id", nullable = false, foreignKey = @ForeignKey(name = "fk_guard_assignments_shift"))
    private GuardShift guardShift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "soldier_id", nullable = false, foreignKey = @ForeignKey(name = "fk_guard_assignments_soldier"))
    private UserAccount soldier;

    @Column(name = "acknowledged_at", columnDefinition = "timestamptz")
    private Instant acknowledgedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuardAssignmentStatus status = GuardAssignmentStatus.ASSIGNED;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_condition", length = 30)
    private GuardResultCondition resultCondition;

    @Column(name = "result_content", length = 2000)
    private String resultContent;

    @Column(name = "incident_note", length = 2000)
    private String incidentNote;

    @Column(name = "completed_at", columnDefinition = "timestamptz")
    private Instant completedAt;
}
