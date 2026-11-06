package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.dto.WorkScheduleDtos.WorkItemResponse;
import com.qldq.qldqapi.security.AuthenticatedUser;
import com.qldq.qldqapi.service.WorkScheduleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-items")
public class WorkItemController {
    private final WorkScheduleService service;

    public WorkItemController(WorkScheduleService service) {
        this.service = service;
    }

    @GetMapping("/{itemId}")
    public WorkItemResponse get(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long itemId
    ) {
        return service.getAssignedItem(actor.id(), itemId);
    }
}
