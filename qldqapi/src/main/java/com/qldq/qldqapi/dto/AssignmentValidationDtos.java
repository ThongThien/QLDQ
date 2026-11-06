package com.qldq.qldqapi.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public final class AssignmentValidationDtos {
    private AssignmentValidationDtos() {
    }

    public record GuardConflictRequest(
            @NotNull OffsetDateTime startAt,
            @NotNull OffsetDateTime endAt,
            @NotEmpty List<@NotNull Long> soldierIds,
            Long excludeGuardShiftId
    ) {
    }

    public record WorkConflictRequest(
            @NotNull LocalDate workDate,
            LocalTime startTime,
            LocalTime endTime,
            boolean allDay,
            @NotEmpty List<@NotNull Long> soldierIds,
            Long excludeWorkItemId
    ) {
    }

    public record ConflictWarning(
            Long soldierId,
            String soldierName,
            String conflictType,
            Long referenceId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String message
    ) {
    }

    public record AssignmentValidationResponse(List<ConflictWarning> warnings) {
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
