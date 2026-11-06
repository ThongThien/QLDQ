package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.dto.NotificationDtos.NotificationResponse;
import com.qldq.qldqapi.dto.NotificationDtos.UnreadCountResponse;
import com.qldq.qldqapi.dto.PagedResponse;
import com.qldq.qldqapi.security.AuthenticatedUser;
import com.qldq.qldqapi.service.NotificationService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public PagedResponse<NotificationResponse> list(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(actor.id(), PageRequests.of(page, size, Sort.by("createdAt").descending()));
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal AuthenticatedUser actor) {
        return service.unreadCount(actor.id());
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long notificationId
    ) {
        return service.markRead(actor.id(), notificationId);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal AuthenticatedUser actor) {
        service.markAllRead(actor.id());
    }
}
