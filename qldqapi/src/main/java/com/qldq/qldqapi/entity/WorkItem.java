package com.qldq.qldqapi.entity;

import com.qldq.qldqapi.config.BusinessTime;
import com.qldq.qldqapi.entity.enums.OccurrenceStatus;
import com.qldq.qldqapi.entity.enums.WorkPeriod;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "work_items",
        indexes = {
                @Index(name = "idx_work_items_schedule_date", columnList = "work_schedule_id,work_date"),
                @Index(name = "idx_work_items_date_period", columnList = "work_date,period")
        }
)
public class WorkItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_schedule_id", nullable = false, foreignKey = @ForeignKey(name = "fk_work_items_schedule"))
    private WorkSchedule workSchedule;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkPeriod period = WorkPeriod.CUSTOM;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 200)
    private String leader;

    @Column(name = "participants_description", length = 500)
    private String participantsDescription;

    @Column(length = 200)
    private String location;

    @Column(length = 1000)
    private String preparation;

    @Column(name = "duty_leader", length = 200)
    private String dutyLeader;

    @Column(length = 2000)
    private String note;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "workItem", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkItemAssignment> assignments = new ArrayList<>();

    @AssertTrue(message = "workDate must be inside its schedule week")
    public boolean isInsideScheduleWeek() {
        if (workSchedule == null || workDate == null
                || workSchedule.getWeekStartDate() == null || workSchedule.getWeekEndDate() == null) {
            return true;
        }
        return !workDate.isBefore(workSchedule.getWeekStartDate())
                && !workDate.isAfter(workSchedule.getWeekEndDate());
    }

    @AssertTrue(message = "Timed work items require different start and end times")
    public boolean isTimeRangeValid() {
        if (allDay) {
            return startTime == null && endTime == null;
        }
        return startTime == null || endTime == null || !endTime.equals(startTime);
    }

    public OccurrenceStatus getOccurrenceStatus(Instant now) {
        LocalTime effectiveStart = allDay ? LocalTime.MIN : startTime;
        if (effectiveStart == null) return OccurrenceStatus.UPCOMING;
        ZonedDateTime start = workDate.atTime(effectiveStart).atZone(BusinessTime.ZONE_ID);
        ZonedDateTime end = allDay
                ? workDate.plusDays(1).atStartOfDay(BusinessTime.ZONE_ID)
                : workDate.atTime(endTime).atZone(BusinessTime.ZONE_ID);
        if (!end.isAfter(start)) end = end.plusDays(1);
        Instant current = now;
        if (current.isBefore(start.toInstant())) return OccurrenceStatus.UPCOMING;
        if (current.isBefore(end.toInstant())) return OccurrenceStatus.ONGOING;
        return OccurrenceStatus.COMPLETED;
    }
}
