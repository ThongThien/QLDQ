package com.qldq.qldqapi.entity;

import com.qldq.qldqapi.entity.enums.GuardShiftStatus;
import com.qldq.qldqapi.entity.enums.OccurrenceStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BusinessTimeRulesTest {
    @Test
    void classifiesBoundariesAndOvernightShift() {
        GuardShift shift = new GuardShift();
        shift.setStartAt(OffsetDateTime.parse("2026-11-05T23:00:00+07:00"));
        shift.setEndAt(OffsetDateTime.parse("2026-11-06T01:00:00+07:00"));

        assertEquals(OccurrenceStatus.UPCOMING, shift.getOccurrenceStatus(Instant.parse("2026-11-05T15:59:59Z")));
        assertEquals(OccurrenceStatus.ONGOING, shift.getOccurrenceStatus(Instant.parse("2026-11-05T16:00:00Z")));
        assertEquals(OccurrenceStatus.ONGOING, shift.getOccurrenceStatus(Instant.parse("2026-11-05T17:00:00Z")));
        assertEquals(OccurrenceStatus.COMPLETED, shift.getOccurrenceStatus(Instant.parse("2026-11-05T18:00:00Z")));
        assertEquals(OccurrenceStatus.COMPLETED, shift.getOccurrenceStatus(Instant.parse("2026-11-05T18:00:01Z")));

        shift.setStatus(GuardShiftStatus.CANCELLED);
        assertEquals(GuardShiftStatus.CANCELLED, shift.getStatus());
    }

    @Test
    void acceptsOnlyMondayToSundayWorkWeek() {
        WorkSchedule valid = new WorkSchedule();
        valid.setWeekStartDate(LocalDate.of(2026, 11, 2));
        valid.setWeekEndDate(LocalDate.of(2026, 11, 8));
        assertTrue(valid.isWeekRangeValid());

        valid.setWeekEndDate(LocalDate.of(2026, 11, 9));
        assertFalse(valid.isWeekRangeValid());
    }

    @Test
    void classifiesTimedAndOvernightWorkItemsAtExactBoundaries() {
        WorkItem item = new WorkItem();
        item.setWorkDate(LocalDate.of(2026, 11, 6));
        item.setStartTime(LocalTime.of(23, 0));
        item.setEndTime(LocalTime.of(1, 0));

        assertEquals(OccurrenceStatus.UPCOMING, item.getOccurrenceStatus(Instant.parse("2026-11-06T15:59:59Z")));
        assertEquals(OccurrenceStatus.ONGOING, item.getOccurrenceStatus(Instant.parse("2026-11-06T16:00:00Z")));
        assertEquals(OccurrenceStatus.ONGOING, item.getOccurrenceStatus(Instant.parse("2026-11-06T17:59:59Z")));
        assertEquals(OccurrenceStatus.COMPLETED, item.getOccurrenceStatus(Instant.parse("2026-11-06T18:00:00Z")));
        assertTrue(item.isTimeRangeValid());
    }
}
