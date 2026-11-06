package com.qldq.qldqapi.repository;

import com.qldq.qldqapi.entity.WorkItemAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;

import java.util.List;

public interface WorkItemAssignmentRepository extends JpaRepository<WorkItemAssignment, Long> {
    List<WorkItemAssignment> findAllByWorkItemId(Long workItemId);

    List<WorkItemAssignment> findAllBySoldierId(Long soldierId);

    @Query("""
            select a from WorkItemAssignment a
            join fetch a.workItem w
            join fetch w.workSchedule
            join fetch a.soldier s
            where s.id in :soldierIds
              and w.workDate between :fromDate and :toDate
              and (:excludeItemId is null or w.id <> :excludeItemId)
            """)
    List<WorkItemAssignment> findConflictCandidates(
            @Param("soldierIds") Collection<Long> soldierIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("excludeItemId") Long excludeItemId
    );
}
