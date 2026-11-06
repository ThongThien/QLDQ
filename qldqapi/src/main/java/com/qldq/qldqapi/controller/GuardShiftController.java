package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.dto.GuardDtos.*;
import com.qldq.qldqapi.dto.PagedResponse;
import com.qldq.qldqapi.entity.enums.GuardShiftStatus;
import com.qldq.qldqapi.service.GuardShiftService;
import com.qldq.qldqapi.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/guard-shifts")
public class GuardShiftController {
    private final GuardShiftService guardShiftService;

    public GuardShiftController(GuardShiftService guardShiftService) {
        this.guardShiftService = guardShiftService;
    }

    @GetMapping
    public PagedResponse<GuardShiftResponse> search(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) GuardShiftStatus status,
            @RequestParam(required = false) Long soldierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return guardShiftService.search(actor.id(), from, to, status, soldierId,
                PageRequests.of(page, size, Sort.by("startAt").ascending()));
    }

    @GetMapping("/{shiftId}")
    public GuardShiftResponse get(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long shiftId
    ) {
        return guardShiftService.get(actor.id(), shiftId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardShiftResponse create(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody GuardShiftRequest request
    ) {
        return guardShiftService.create(actor.id(), request);
    }

    @PostMapping("/generate-week")
    @ResponseStatus(HttpStatus.CREATED)
    public GuardWeekResponse generateWeek(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody GuardWeekRequest request
    ) {
        return guardShiftService.generateWeek(actor.id(), request);
    }

    @PutMapping("/{shiftId}")
    public GuardShiftResponse update(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long shiftId,
            @Valid @RequestBody GuardShiftRequest request
    ) {
        return guardShiftService.update(actor.id(), shiftId, request);
    }

    @PatchMapping("/{shiftId}/status")
    public GuardShiftResponse changeStatus(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long shiftId,
            @Valid @RequestBody GuardStatusRequest request
    ) {
        return guardShiftService.changeStatus(actor.id(), shiftId, request);
    }

    @DeleteMapping("/{shiftId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long shiftId
    ) {
        guardShiftService.delete(actor.id(), shiftId);
    }

    @PostMapping("/{shiftId}/acknowledgement")
    public GuardShiftResponse acknowledge(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long shiftId
    ) {
        return guardShiftService.acknowledge(actor.id(), shiftId);
    }

    @PostMapping("/{shiftId}/result")
    public GuardShiftResponse complete(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long shiftId,
            @Valid @RequestBody GuardResultRequest request
    ) {
        return guardShiftService.complete(actor.id(), shiftId, request);
    }
}
