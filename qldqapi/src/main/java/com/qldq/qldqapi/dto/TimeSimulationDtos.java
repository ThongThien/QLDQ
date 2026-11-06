package com.qldq.qldqapi.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public final class TimeSimulationDtos {
    private TimeSimulationDtos() {
    }

    public record SimulationStatus(
            boolean enabled,
            OffsetDateTime currentTime,
            String zoneId
    ) {
    }

    public record SetSimulationRequest(@NotNull OffsetDateTime simulatedAt) {
    }

    public record AdvanceSimulationRequest(long hours, long days) {
    }
}
