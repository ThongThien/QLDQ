package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.dto.DashboardResponse;
import com.qldq.qldqapi.service.DashboardService;
import com.qldq.qldqapi.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse get(@AuthenticationPrincipal AuthenticatedUser actor) {
        return dashboardService.get(actor.id());
    }
}
