package com.qldq.qldqapi.dto;

import com.qldq.qldqapi.entity.enums.UserRole;

import java.time.Instant;

public final class SoldierDtos {
    private SoldierDtos() {
    }

    public record SoldierResponse(
            Long id,
            String username,
            String fullName,
            UserRole role,
            String rankOrPosition,
            boolean active,
            long assignedShiftCount,
            long completedShiftCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
