package com.qldq.qldqapi.repository;

import com.qldq.qldqapi.entity.WorkSchedule;
import com.qldq.qldqapi.entity.enums.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.qldq.qldqapi.entity.enums.ScheduleStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {
    Optional<WorkSchedule> findByScheduleTypeAndWeekStartDate(ScheduleType scheduleType, LocalDate weekStartDate);

    List<WorkSchedule> findAllByScheduleTypeOrderByWeekStartDateDesc(ScheduleType scheduleType);

    Page<WorkSchedule> findAllByScheduleType(ScheduleType scheduleType, Pageable pageable);

    Page<WorkSchedule> findAllByScheduleTypeAndStatus(
            ScheduleType scheduleType, ScheduleStatus status, Pageable pageable
    );

    @Query("""
            select distinct s from WorkSchedule s
            join s.items item
            join item.assignments assignment
            where s.scheduleType = :scheduleType
              and s.status = :status
              and assignment.soldier.id = :soldierId
            """)
    Page<WorkSchedule> findAssignedPublished(
            @Param("scheduleType") ScheduleType scheduleType,
            @Param("status") ScheduleStatus status,
            @Param("soldierId") Long soldierId,
            Pageable pageable
    );
}
