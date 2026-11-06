package com.qldq.qldqapi.dto;

import com.qldq.qldqapi.entity.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public final class GuardDtos {
    private GuardDtos() {
    }

    public record GuardShiftRequest(
            @NotNull OffsetDateTime startAt,
            @NotNull OffsetDateTime endAt,
            @NotBlank @Size(max = 150) String location,
            @Size(max = 1000) String note,
            @NotNull GuardShiftStatus status,
            @NotEmpty List<@NotNull Long> soldierIds
    ) {
    }

    public record GuardWeekRequest(@NotNull LocalDate weekStartDate) {
    }

    public record GuardWeekResponse(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            int createdShiftCount,
            int assignmentCount
    ) {
    }

    public record GuardStatusRequest(@NotNull GuardShiftStatus status) {
    }

    public record GuardResultRequest(
            @NotNull GuardResultCondition resultCondition,
            @NotBlank @Size(max = 2000) String resultContent,
            @Size(max = 2000) String incidentNote
    ) {
    }

    public record SoldierBrief(Long id, String username, String fullName, String rankOrPosition, boolean active) {
    }

    public record AssignmentResponse(
            Long id,
            SoldierBrief soldier,
            Instant acknowledgedAt,
            GuardAssignmentStatus status,
            GuardResultCondition resultCondition,
            String resultContent,
            String incidentNote,
            Instant completedAt
    ) {
    }

    public record GuardShiftResponse(
            Long id,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            String note,
            GuardShiftStatus managementStatus,
            OccurrenceStatus occurrenceStatus,
            String status,
            SoldierBrief createdBy,
            List<SoldierBrief> assignedSoldiers,
            List<AssignmentResponse> assignments,
            boolean acknowledged,
            boolean hasIncident,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
