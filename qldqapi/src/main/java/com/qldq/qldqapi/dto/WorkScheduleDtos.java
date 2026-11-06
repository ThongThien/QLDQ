package com.qldq.qldqapi.dto;

import com.qldq.qldqapi.entity.enums.ScheduleStatus;
import com.qldq.qldqapi.entity.enums.ScheduleType;
import com.qldq.qldqapi.entity.enums.WorkPeriod;
import com.qldq.qldqapi.entity.enums.OccurrenceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class WorkScheduleDtos {
    private WorkScheduleDtos() {
    }

    public record ScheduleRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull LocalDate weekStartDate,
            @Size(max = 200) String scopeArea,
            @Size(max = 3000) String generalNote,
            @NotNull ScheduleStatus status
    ) {
    }

    public record WorkItemRequest(
            @NotNull LocalDate workDate,
            @NotNull WorkPeriod period,
            LocalTime startTime,
            LocalTime endTime,
            boolean allDay,
            @NotBlank @Size(max = 2000) String content,
            @Size(max = 200) String leader,
            @Size(max = 500) String participantsDescription,
            @Size(max = 200) String location,
            @Size(max = 1000) String preparation,
            @Size(max = 200) String dutyLeader,
            @Size(max = 2000) String note,
            int sortOrder,
            List<@NotNull Long> soldierIds
    ) {
    }

    public record WorkItemResponse(
            Long id,
            Long scheduleId,
            LocalDate workDate,
            WorkPeriod period,
            LocalTime startTime,
            LocalTime endTime,
            boolean allDay,
            String content,
            String leader,
            String participantsDescription,
            String location,
            String preparation,
            String dutyLeader,
            String note,
            int sortOrder,
            OccurrenceStatus occurrenceStatus,
            List<GuardDtos.SoldierBrief> assignedSoldiers,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ScheduleResponse(
            Long id,
            ScheduleType scheduleType,
            String title,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            String weekLabel,
            String scopeArea,
            String generalNote,
            ScheduleStatus status,
            GuardDtos.SoldierBrief createdBy,
            List<WorkItemResponse> items,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
