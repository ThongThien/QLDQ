package com.qldq.qldqapi.config;

import org.springframework.stereotype.Component;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Time source used only for schedule occurrence, dashboard and reminder calculations.
 * Audit timestamps, authentication and sessions deliberately continue using system time.
 */
@Component
public class BusinessClock {
    private final Clock realClock;
    private final AtomicReference<Instant> simulatedInstant = new AtomicReference<>();

    public BusinessClock() {
        this(Clock.systemUTC());
    }

    BusinessClock(Clock realClock) {
        this.realClock = realClock;
    }

    public Instant instant() {
        Instant simulated = simulatedInstant.get();
        return simulated != null ? simulated : realClock.instant();
    }

    public ZonedDateTime now() {
        return instant().atZone(BusinessTime.ZONE_ID);
    }

    public LocalDate today() {
        return now().toLocalDate();
    }

    public boolean isSimulating() {
        return simulatedInstant.get() != null;
    }

    public void enable(OffsetDateTime value) {
        simulatedInstant.set(value.toInstant());
    }

    public void advance(Duration duration) {
        simulatedInstant.updateAndGet(current -> {
            if (current == null) {
                throw new IllegalStateException("Chế độ mô phỏng chưa được bật");
            }
            return current.plus(duration);
        });
    }

    public void reset() {
        simulatedInstant.set(null);
    }
}
