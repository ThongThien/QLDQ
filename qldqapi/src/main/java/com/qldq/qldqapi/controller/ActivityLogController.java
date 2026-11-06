package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.dto.ActivityLogDtos.ActivityLogResponse;
import com.qldq.qldqapi.dto.PagedResponse;
import com.qldq.qldqapi.entity.enums.ActivityAction;
import com.qldq.qldqapi.entity.enums.ActivityTargetType;
import com.qldq.qldqapi.security.AuthenticatedUser;
import com.qldq.qldqapi.service.ActivityLogService;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/activity-logs")
public class ActivityLogController {
    private final ActivityLogService service;

    public ActivityLogController(ActivityLogService service) {
        this.service = service;
    }

    @GetMapping
    public PagedResponse<ActivityLogResponse> search(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) ActivityAction action,
            @RequestParam(required = false) ActivityTargetType targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.search(actor.id(), from, to, action, targetType, targetId,
                PageRequests.of(page, size, Sort.by("createdAt").descending()));
    }
}
