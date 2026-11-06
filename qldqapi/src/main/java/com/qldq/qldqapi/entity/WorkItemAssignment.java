package com.qldq.qldqapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "work_item_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_work_item_assignments_item_soldier",
                columnNames = {"work_item_id", "soldier_id"}
        ),
        indexes = @Index(name = "idx_work_item_assignments_soldier", columnList = "soldier_id")
)
public class WorkItemAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_work_item_assignments_item"))
    private WorkItem workItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "soldier_id", nullable = false, foreignKey = @ForeignKey(name = "fk_work_item_assignments_soldier"))
    private UserAccount soldier;
}
