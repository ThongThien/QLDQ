package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.dto.PagedResponse;
import com.qldq.qldqapi.dto.SoldierDtos.SoldierResponse;
import com.qldq.qldqapi.dto.SoldierDtos.SoldierCreateRequest;
import com.qldq.qldqapi.service.SoldierService;
import com.qldq.qldqapi.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/soldiers")
public class SoldierController {
    private final SoldierService soldierService;

    public SoldierController(SoldierService soldierService) {
        this.soldierService = soldierService;
    }

    @GetMapping
    public PagedResponse<SoldierResponse> search(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return soldierService.search(actor.id(), search, active,
                PageRequests.of(page, size, Sort.by("fullName").ascending()));
    }

    @GetMapping("/{soldierId}")
    public SoldierResponse get(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long soldierId
    ) {
        return soldierService.get(actor.id(), soldierId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SoldierResponse create(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody SoldierCreateRequest request
    ) {
        return soldierService.create(actor.id(), request);
    }
}
