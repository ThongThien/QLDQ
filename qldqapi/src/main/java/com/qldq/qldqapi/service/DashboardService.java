package com.qldq.qldqapi.service;

import com.qldq.qldqapi.config.BusinessClock;
import com.qldq.qldqapi.config.BusinessTime;
import com.qldq.qldqapi.dto.DashboardResponse;
import com.qldq.qldqapi.entity.UserAccount;
import com.qldq.qldqapi.entity.WorkItem;
import com.qldq.qldqapi.entity.enums.OccurrenceStatus;
import com.qldq.qldqapi.entity.enums.GuardResultCondition;
import com.qldq.qldqapi.entity.enums.GuardShiftStatus;
import com.qldq.qldqapi.entity.enums.UserRole;
import com.qldq.qldqapi.repository.GuardAssignmentRepository;
import com.qldq.qldqapi.repository.GuardShiftRepository;
import com.qldq.qldqapi.repository.NotificationRepository;
import com.qldq.qldqapi.repository.UserAccountRepository;
import com.qldq.qldqapi.repository.WorkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private static final List<GuardResultCondition> INCIDENT_CONDITIONS = List.of(
            GuardResultCondition.INCIDENT, GuardResultCondition.REQUIRES_REPORT
    );

    private final UserAccountRepository userRepository;
    private final GuardShiftRepository shiftRepository;
    private final GuardAssignmentRepository assignmentRepository;
    private final WorkItemRepository workItemRepository;
    private final NotificationRepository notificationRepository;
    private final UserAccessService accessService;
    private final BusinessClock businessClock;

    public DashboardService(
            UserAccountRepository userRepository,
            GuardShiftRepository shiftRepository,
            GuardAssignmentRepository assignmentRepository,
            WorkItemRepository workItemRepository,
            NotificationRepository notificationRepository,
            UserAccessService accessService,
            BusinessClock businessClock
    ) {
        this.userRepository = userRepository;
        this.shiftRepository = shiftRepository;
        this.assignmentRepository = assignmentRepository;
        this.workItemRepository = workItemRepository;
        this.notificationRepository = notificationRepository;
        this.accessService = accessService;
        this.businessClock = businessClock;
    }

    public DashboardResponse get(Long actorId) {
        UserAccount actor = accessService.requireActiveUser(actorId);
        ZonedDateTime localNow = businessClock.now();
        LocalDate today = localNow.toLocalDate();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        OffsetDateTime dayStart = today.atStartOfDay(BusinessTime.ZONE_ID).toOffsetDateTime();
        OffsetDateTime dayEnd = today.plusDays(1).atStartOfDay(BusinessTime.ZONE_ID).toOffsetDateTime();
        OffsetDateTime weekStartAt = weekStart.atStartOfDay(BusinessTime.ZONE_ID).toOffsetDateTime();
        OffsetDateTime weekEndExclusive = weekEnd.plusDays(1).atStartOfDay(BusinessTime.ZONE_ID).toOffsetDateTime();
        OffsetDateTime now = localNow.toOffsetDateTime();
        Long soldierId = actor.getRole() == UserRole.SOLDIER ? actor.getId() : null;

        long shiftsToday = actor.getRole() == UserRole.COMMANDER
                ? shiftRepository.countAllOverlapping(dayStart, dayEnd) : 0;
        long shiftsThisWeek = actor.getRole() == UserRole.COMMANDER
                ? shiftRepository.countAllOverlapping(weekStartAt, weekEndExclusive)
                : shiftRepository.countForSoldierInWeek(actor.getId(), weekStartAt, weekEndExclusive);
        long completedShifts = actor.getRole() == UserRole.COMMANDER
                ? shiftRepository.countCompletedInWeek(weekStartAt, now)
                : shiftRepository.countCompletedForSoldier(actor.getId(), weekStartAt, now);
        long ongoingShifts = actor.getRole() == UserRole.COMMANDER
                ? shiftRepository.countOngoing(now)
                : shiftRepository.countOngoingForSoldier(actor.getId(), now);
        long upcomingShifts = actor.getRole() == UserRole.COMMANDER
                ? shiftRepository.countUpcomingInWeek(now, weekEndExclusive)
                : shiftRepository.countUpcomingForSoldier(actor.getId(), now, weekEndExclusive);
        long cancelledShifts = actor.getRole() == UserRole.COMMANDER
                ? shiftRepository.countStatusOverlapping(GuardShiftStatus.CANCELLED, weekStartAt, weekEndExclusive)
                : shiftRepository.countCancelledForSoldier(actor.getId(), weekStartAt, weekEndExclusive);
        List<WorkItem> weeklyTasks = workItemRepository.findWeeklyTasksForDashboard(soldierId, weekStart, weekEnd);
        long weeklyWorkItems = weeklyTasks.size();
        long completedWorkItems = weeklyTasks.stream()
                .filter(item -> item.getOccurrenceStatus(businessClock.instant()) == OccurrenceStatus.COMPLETED)
                .count();
        int progress = weeklyWorkItems == 0 ? 0
                : (int) Math.min(100, Math.round(completedWorkItems * 100.0 / weeklyWorkItems));

        return new DashboardResponse(
                actor.getRole(), now, businessClock.isSimulating(), today, weekStart, weekEnd,
                actor.getRole() == UserRole.COMMANDER ? userRepository.countByRole(UserRole.SOLDIER) : 0,
                actor.getRole() == UserRole.COMMANDER ? userRepository.countByRoleAndActiveTrue(UserRole.SOLDIER) : 0,
                shiftsToday, shiftsThisWeek, completedShifts, ongoingShifts, upcomingShifts, cancelledShifts,
                assignmentRepository.countMissingAcknowledgements(soldierId, now, weekEndExclusive),
                assignmentRepository.countIncidentsInWeek(soldierId, INCIDENT_CONDITIONS, weekStartAt, weekEndExclusive),
                weeklyWorkItems, completedWorkItems, progress,
                notificationRepository.countByRecipientIdAndReadAtIsNull(actor.getId())
        );
    }
}
