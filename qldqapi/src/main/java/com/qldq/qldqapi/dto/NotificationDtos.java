package com.qldq.qldqapi.dto;

import com.qldq.qldqapi.entity.enums.NotificationReferenceType;
import com.qldq.qldqapi.entity.enums.NotificationType;

import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            String title,
            String content,
            NotificationType type,
            NotificationReferenceType referenceType,
            Long referenceId,
            Instant createdAt,
            Instant readAt
    ) {
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}
