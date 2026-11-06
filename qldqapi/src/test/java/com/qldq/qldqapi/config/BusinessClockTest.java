package com.qldq.qldqapi.config;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class BusinessClockTest {
    @Test
    void enablesAdvancesAndReturnsToRealTime() {
        Instant real = Instant.parse("2026-11-05T07:00:00Z");
        BusinessClock clock = new BusinessClock(Clock.fixed(real, ZoneOffset.UTC));

        clock.enable(OffsetDateTime.parse("2026-11-06T03:00:00+07:00"));
        assertTrue(clock.isSimulating());
        assertEquals(LocalDateTime.parse("2026-11-06T03:00:00"), clock.now().toLocalDateTime());

        clock.advance(Duration.ofHours(25));
        assertEquals(LocalDateTime.parse("2026-11-07T04:00:00"), clock.now().toLocalDateTime());

        clock.reset();
        assertFalse(clock.isSimulating());
        assertEquals(real, clock.instant());
    }
}
