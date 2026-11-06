package com.qldq.qldqapi.entity;

import com.qldq.qldqapi.entity.enums.ScheduleStatus;
import com.qldq.qldqapi.entity.enums.ScheduleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "work_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_work_schedules_type_week",
                columnNames = {"schedule_type", "week_start_date"}
        ),
        indexes = @Index(name = "idx_work_schedules_week_status", columnList = "week_start_date,status")
)
public class WorkSchedule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 30)
    private ScheduleType scheduleType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Column(name = "scope_area", length = 200)
    private String scopeArea;

    @Column(name = "general_note", length = 3000)
    private String generalNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false, foreignKey = @ForeignKey(name = "fk_work_schedules_created_by"))
    private UserAccount createdBy;

    @OneToMany(mappedBy = "workSchedule", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("workDate ASC, sortOrder ASC")
    private List<WorkItem> items = new ArrayList<>();

    @AssertTrue(message = "A work week must start on Monday and end on the following Sunday")
    public boolean isWeekRangeValid() {
        if (weekStartDate == null || weekEndDate == null) {
            return true;
        }
        return weekStartDate.getDayOfWeek() == DayOfWeek.MONDAY
                && weekEndDate.getDayOfWeek() == DayOfWeek.SUNDAY
                && weekEndDate.equals(weekStartDate.plusDays(6));
    }
}
