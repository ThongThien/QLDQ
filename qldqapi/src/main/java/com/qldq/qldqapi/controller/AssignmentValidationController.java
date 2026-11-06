package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.dto.AssignmentValidationDtos.*;
import com.qldq.qldqapi.security.AuthenticatedUser;
import com.qldq.qldqapi.service.AssignmentValidationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assignment-validation")
public class AssignmentValidationController {
    private final AssignmentValidationService service;

    public AssignmentValidationController(AssignmentValidationService service) {
        this.service = service;
    }

    @PostMapping("/guard-shift")
    public AssignmentValidationResponse guard(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody GuardConflictRequest request
    ) {
        return service.validateGuard(actor.id(), request);
    }

    @PostMapping("/work-item")
    public AssignmentValidationResponse work(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody WorkConflictRequest request
    ) {
        return service.validateWork(actor.id(), request);
    }
}
