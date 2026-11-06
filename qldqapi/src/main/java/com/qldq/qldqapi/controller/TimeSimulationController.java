package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.config.BusinessClock;
import com.qldq.qldqapi.config.BusinessTime;
import com.qldq.qldqapi.dto.TimeSimulationDtos.AdvanceSimulationRequest;
import com.qldq.qldqapi.dto.TimeSimulationDtos.SetSimulationRequest;
import com.qldq.qldqapi.dto.TimeSimulationDtos.SimulationStatus;
import com.qldq.qldqapi.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@Profile("dev")
@RequestMapping("/api/dev/time")
public class TimeSimulationController {
    private final BusinessClock businessClock;

    public TimeSimulationController(BusinessClock businessClock) {
        this.businessClock = businessClock;
    }

    @GetMapping
    public SimulationStatus status() {
        return statusResponse();
    }

    @PutMapping
    public SimulationStatus enable(@Valid @RequestBody SetSimulationRequest request) {
        businessClock.enable(request.simulatedAt());
        return statusResponse();
    }

    @PostMapping("/advance")
    public SimulationStatus advance(@RequestBody AdvanceSimulationRequest request) {
        if (request.hours() == 0 && request.days() == 0) {
            throw new BusinessException("Khoảng thời gian thay đổi phải khác 0");
        }
        try {
            businessClock.advance(Duration.ofDays(request.days()).plusHours(request.hours()));
        } catch (IllegalStateException exception) {
            throw new BusinessException(exception.getMessage());
        }
        return statusResponse();
    }

    @DeleteMapping
    public SimulationStatus reset() {
        businessClock.reset();
        return statusResponse();
    }

    private SimulationStatus statusResponse() {
        return new SimulationStatus(businessClock.isSimulating(), businessClock.now().toOffsetDateTime(),
                BusinessTime.ZONE_ID.getId());
    }
}
