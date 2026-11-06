package com.qldq.qldqapi.repository;

import com.qldq.qldqapi.entity.GuardAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;

import java.util.List;
import java.util.Optional;

public interface GuardAssignmentRepository extends JpaRepository<GuardAssignment, Long> {
    List<GuardAssignment> findAllBySoldierIdOrderByGuardShiftStartAtDesc(Long soldierId);

    List<GuardAssignment> findAllByGuardShiftId(Long guardShiftId);

    Optional<GuardAssignment> findByGuardShiftIdAndSoldierId(Long guardShiftId, Long soldierId);

    long countBySoldierId(Long soldierId);

    long countBySoldierIdAndStatus(Long soldierId, com.qldq.qldqapi.entity.enums.GuardAssignmentStatus status);

    long countByAcknowledgedAtIsNullAndGuardShiftStatus(com.qldq.qldqapi.entity.enums.GuardShiftStatus status);

    long countByResultConditionIn(java.util.Collection<com.qldq.qldqapi.entity.enums.GuardResultCondition> conditions);

    @Query("""
            select count(a) from GuardAssignment a join a.guardShift g
            where a.acknowledgedAt is null
              and g.status = com.qldq.qldqapi.entity.enums.GuardShiftStatus.PUBLISHED
              and g.endAt > :now and g.startAt < :weekEndExclusive
              and (:soldierId is null or a.soldier.id = :soldierId)
            """)
    long countMissingAcknowledgements(
            @Param("soldierId") Long soldierId,
            @Param("now") OffsetDateTime now,
            @Param("weekEndExclusive") OffsetDateTime weekEndExclusive
    );

    @Query("""
            select count(a) from GuardAssignment a join a.guardShift g
            where a.resultCondition in :conditions
              and g.startAt < :weekEndExclusive and g.endAt > :weekStart
              and (:soldierId is null or a.soldier.id = :soldierId)
            """)
    long countIncidentsInWeek(
            @Param("soldierId") Long soldierId,
            @Param("conditions") Collection<com.qldq.qldqapi.entity.enums.GuardResultCondition> conditions,
            @Param("weekStart") OffsetDateTime weekStart,
            @Param("weekEndExclusive") OffsetDateTime weekEndExclusive
    );

    @Query("""
            select a from GuardAssignment a
            join fetch a.guardShift g
            join fetch a.soldier s
            where s.id in :soldierIds
              and g.status <> com.qldq.qldqapi.entity.enums.GuardShiftStatus.CANCELLED
              and g.startAt < :endAt and g.endAt > :startAt
              and (:excludeShiftId is null or g.id <> :excludeShiftId)
            """)
    List<GuardAssignment> findConflictingAssignments(
            @Param("soldierIds") Collection<Long> soldierIds,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt,
            @Param("excludeShiftId") Long excludeShiftId
    );
}
