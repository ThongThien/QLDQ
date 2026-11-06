package com.qldq.qldqapi.dto;

import com.qldq.qldqapi.entity.enums.ActivityAction;
import com.qldq.qldqapi.entity.enums.ActivityTargetType;

import java.time.Instant;

public final class ActivityLogDtos {
    private ActivityLogDtos() {
    }

    public record ActivityActor(Long id, String username, String fullName, String role) {
    }

    public record ActivityLogResponse(
            Long id,
            ActivityActor actor,
            ActivityAction action,
            ActivityTargetType targetType,
            Long targetId,
            String description,
            Instant createdAt
    ) {
    }
}
