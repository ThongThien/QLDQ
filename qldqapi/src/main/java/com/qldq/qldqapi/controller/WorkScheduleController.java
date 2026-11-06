package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.dto.PagedResponse;
import com.qldq.qldqapi.dto.WorkScheduleDtos.*;
import com.qldq.qldqapi.entity.enums.ScheduleType;
import com.qldq.qldqapi.service.WorkScheduleService;
import com.qldq.qldqapi.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-schedules")
public class WorkScheduleController {
    private static final ScheduleType TYPE = ScheduleType.WEEKLY_WORK;
    private final WorkScheduleService service;

    public WorkScheduleController(WorkScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public PagedResponse<ScheduleResponse> list(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(actor.id(), TYPE, mine,
                PageRequests.of(page, size, Sort.by("weekStartDate").descending()));
    }

    @GetMapping("/{scheduleId}")
    public ScheduleResponse get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long scheduleId) {
        return service.get(actor.id(), TYPE, scheduleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create(
            @AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody ScheduleRequest request
    ) {
        return service.create(actor.id(), TYPE, request);
    }

    @PutMapping("/{scheduleId}")
    public ScheduleResponse update(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequest request
    ) {
        return service.update(actor.id(), TYPE, scheduleId, request);
    }

    @PostMapping("/{scheduleId}/publish")
    public ScheduleResponse publish(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long scheduleId) {
        return service.publish(actor.id(), TYPE, scheduleId);
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long scheduleId) {
        service.delete(actor.id(), TYPE, scheduleId);
    }

    @PostMapping("/{scheduleId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkItemResponse addItem(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long scheduleId,
            @Valid @RequestBody WorkItemRequest request
    ) {
        return service.addItem(actor.id(), TYPE, scheduleId, request);
    }

    @PutMapping("/{scheduleId}/items/{itemId}")
    public WorkItemResponse updateItem(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long scheduleId,
            @PathVariable Long itemId,
            @Valid @RequestBody WorkItemRequest request
    ) {
        return service.updateItem(actor.id(), TYPE, scheduleId, itemId, request);
    }

    @DeleteMapping("/{scheduleId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long scheduleId,
            @PathVariable Long itemId
    ) {
        service.deleteItem(actor.id(), TYPE, scheduleId, itemId);
    }
}
