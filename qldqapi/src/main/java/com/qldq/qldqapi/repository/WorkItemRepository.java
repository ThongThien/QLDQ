package com.qldq.qldqapi.repository;

import com.qldq.qldqapi.entity.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;
import com.qldq.qldqapi.entity.enums.ScheduleType;
import com.qldq.qldqapi.entity.enums.ScheduleStatus;
import com.qldq.qldqapi.entity.enums.WorkPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

import java.util.List;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    List<WorkItem> findAllByWorkScheduleIdOrderByWorkDateAscSortOrderAsc(Long workScheduleId);

    @Query("""
            select w from WorkItem w
            where w.workSchedule.scheduleType = :scheduleType
              and (:scheduleStatus is null or w.workSchedule.status = :scheduleStatus)
              and (:workDate is null or w.workDate = :workDate)
              and (:period is null or w.period = :period)
              and (:soldierId is null or (
                    w.workSchedule.status = com.qldq.qldqapi.entity.enums.ScheduleStatus.PUBLISHED
                    and exists (select a.id from WorkItemAssignment a
                                where a.workItem = w and a.soldier.id = :soldierId)))
            """)
    Page<WorkItem> searchByScheduleType(
            @Param("scheduleType") ScheduleType scheduleType,
            @Param("scheduleStatus") ScheduleStatus scheduleStatus,
            @Param("workDate") LocalDate workDate,
            @Param("period") WorkPeriod period,
            @Param("soldierId") Long soldierId,
            Pageable pageable
    );

    @Query("""
            select distinct w from WorkItem w
            join fetch w.workSchedule s
            left join fetch w.assignments a
            left join fetch a.soldier
            where s.status = :status and w.workDate between :fromDate and :toDate
            """)
    List<WorkItem> findReminderCandidates(
            @Param("status") ScheduleStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
            select count(w) from WorkItem w
            where w.workSchedule.scheduleType = com.qldq.qldqapi.entity.enums.ScheduleType.WEEKLY_WORK
              and w.workSchedule.status = com.qldq.qldqapi.entity.enums.ScheduleStatus.PUBLISHED
              and w.workDate between :weekStart and :weekEnd
              and (:soldierId is null or exists (
                    select a.id from WorkItemAssignment a where a.workItem = w and a.soldier.id = :soldierId))
            """)
    long countWeeklyTasks(
            @Param("soldierId") Long soldierId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd
    );

    @Query("""
            select count(w) from WorkItem w
            where w.workSchedule.scheduleType = com.qldq.qldqapi.entity.enums.ScheduleType.WEEKLY_WORK
              and w.workSchedule.status = com.qldq.qldqapi.entity.enums.ScheduleStatus.PUBLISHED
              and w.workDate between :weekStart and :weekEnd
              and (w.workDate < :today or (
                    w.workDate = :today and w.allDay = false and w.endTime is not null and w.endTime <= :localTime))
              and (:soldierId is null or exists (
                    select a.id from WorkItemAssignment a where a.workItem = w and a.soldier.id = :soldierId))
            """)
    long countCompletedWeeklyTasks(
            @Param("soldierId") Long soldierId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd,
            @Param("today") LocalDate today,
            @Param("localTime") java.time.LocalTime localTime
    );

    @Query("""
            select distinct w from WorkItem w
            left join fetch w.assignments assignment
            left join fetch assignment.soldier
            where w.workSchedule.scheduleType = com.qldq.qldqapi.entity.enums.ScheduleType.WEEKLY_WORK
              and w.workSchedule.status = com.qldq.qldqapi.entity.enums.ScheduleStatus.PUBLISHED
              and w.workDate between :weekStart and :weekEnd
              and (:soldierId is null or exists (
                    select a.id from WorkItemAssignment a where a.workItem = w and a.soldier.id = :soldierId))
            """)
    List<WorkItem> findWeeklyTasksForDashboard(
            @Param("soldierId") Long soldierId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd
    );
}
