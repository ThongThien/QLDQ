package com.qldq.qldqapi.dto;

import com.qldq.qldqapi.entity.enums.UserRole;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DashboardResponse(
        UserRole role,
        OffsetDateTime businessTime,
        boolean simulationEnabled,
        LocalDate date,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        long totalSoldiers,
        long totalActiveSoldiers,
        long shiftsToday,
        long shiftsThisWeek,
        long completedShifts,
        long ongoingShifts,
        long upcomingShifts,
        long cancelledShifts,
        long unacknowledgedAssignments,
        long incidentOrReviewAssignments,
        long weeklyWorkItems,
        long completedWorkItems,
        int workCompletionPercent,
        long unreadNotifications
) {
}
