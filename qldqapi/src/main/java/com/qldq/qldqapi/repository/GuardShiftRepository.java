package com.qldq.qldqapi.repository;

import com.qldq.qldqapi.entity.GuardShift;
import com.qldq.qldqapi.entity.enums.GuardShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface GuardShiftRepository extends JpaRepository<GuardShift, Long> {
    List<GuardShift> findAllByStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<GuardShift> findAllByStatusAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
            GuardShiftStatus status,
            OffsetDateTime from,
            OffsetDateTime to
    );

    @Query("""
            select distinct g from GuardShift g
            where g.startAt < :toExclusive and g.endAt > :fromInclusive
              and (:status is null or g.status = :status)
              and (:soldierId is null or exists (
                    select a.id from GuardAssignment a
                    where a.guardShift = g and a.soldier.id = :soldierId))
            """)
    Page<GuardShift> searchOverlapping(
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toExclusive") OffsetDateTime toExclusive,
            @Param("status") GuardShiftStatus status,
            @Param("soldierId") Long soldierId,
            Pageable pageable
    );

    @Query("""
            select count(g) from GuardShift g
            where g.status <> com.qldq.qldqapi.entity.enums.GuardShiftStatus.CANCELLED
              and g.startAt < :toExclusive and g.endAt > :fromInclusive
            """)
    long countActiveOverlapping(
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toExclusive") OffsetDateTime toExclusive
    );

    @Query("""
            select count(g) from GuardShift g
            where g.startAt < :toExclusive and g.endAt > :fromInclusive
            """)
    long countAllOverlapping(
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toExclusive") OffsetDateTime toExclusive
    );

    long countByStartAtGreaterThanEqualAndStartAtLessThan(
            OffsetDateTime fromInclusive,
            OffsetDateTime toExclusive
    );

    @Query("""
            select count(g) from GuardShift g
            where g.status = :status
              and g.startAt < :toExclusive and g.endAt > :fromInclusive
            """)
    long countStatusOverlapping(
            @Param("status") GuardShiftStatus status,
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toExclusive") OffsetDateTime toExclusive
    );

    @Query("""
            select count(g) from GuardShift g
            where g.status = com.qldq.qldqapi.entity.enums.GuardShiftStatus.PUBLISHED
              and g.endAt <= :now and g.endAt > :weekStart
            """)
    long countCompletedInWeek(@Param("weekStart") OffsetDateTime weekStart, @Param("now") OffsetDateTime now);

    @Query("""
            select count(g) from GuardShift g
            where g.status = com.qldq.qldqapi.entity.enums.GuardShiftStatus.PUBLISHED
              and g.startAt <= :now and g.endAt > :now
            """)
    long countOngoing(@Param("now") OffsetDateTime now);

    @Query("""
            select count(g) from GuardShift g
            where g.status = com.qldq.qldqapi.entity.enums.GuardShiftStatus.PUBLISHED
              and g.startAt > :now and g.startAt < :weekEndExclusive
            """)
    long countUpcomingInWeek(@Param("now") OffsetDateTime now, @Param("weekEndExclusive") OffsetDateTime weekEndExclusive);

    @Query("""
            select count(g) from GuardShift g
            where exists (select a.id from GuardAssignment a where a.guardShift = g and a.soldier.id = :soldierId)
              and g.startAt < :toExclusive and g.endAt > :fromInclusive
            """)
    long countForSoldierInWeek(
            @Param("soldierId") Long soldierId,
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toExclusive") OffsetDateTime toExclusive
    );

    @Query("""
            select count(g) from GuardShift g
            where g.status = com.qldq.qldqapi.entity.enums.GuardShiftStatus.PUBLISHED
              and g.startAt > :now and g.startAt < :weekEndExclusive
              and exists (select a.id from GuardAssignment a where a.guardShift = g and a.soldier.id = :soldierId)
            """)
    long countUpcomingForSoldier(
            @Param("soldierId") Long soldierId,
            @Param("now") OffsetDateTime now,
            @Param("weekEndExclusive") OffsetDateTime weekEndExclusive
    );

    @Query("""
            select count(g) from GuardShift g
            where g.status = com.qldq.qldqapi.entity.enums.GuardShiftStatus.PUBLISHED
              and g.startAt <= :now and g.endAt > :now
              and exists (select a.id from GuardAssignment a where a.guardShift = g and a.soldier.id = :soldierId)
            """)
    long countOngoingForSoldier(@Param("soldierId") Long soldierId, @Param("now") OffsetDateTime now);

    @Query("""
            select count(g) from GuardShift g
            where g.status = com.qldq.qldqapi.entity.enums.GuardShiftStatus.PUBLISHED
              and g.endAt <= :now and g.endAt > :weekStart
              and exists (select a.id from GuardAssignment a where a.guardShift = g and a.soldier.id = :soldierId)
            """)
    long countCompletedForSoldier(
            @Param("soldierId") Long soldierId,
            @Param("weekStart") OffsetDateTime weekStart,
            @Param("now") OffsetDateTime now
    );

    @Query("""
            select count(g) from GuardShift g
            where g.status = com.qldq.qldqapi.entity.enums.GuardShiftStatus.CANCELLED
              and g.startAt < :toExclusive and g.endAt > :fromInclusive
              and exists (select a.id from GuardAssignment a where a.guardShift = g and a.soldier.id = :soldierId)
            """)
    long countCancelledForSoldier(
            @Param("soldierId") Long soldierId,
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toExclusive") OffsetDateTime toExclusive
    );
}
