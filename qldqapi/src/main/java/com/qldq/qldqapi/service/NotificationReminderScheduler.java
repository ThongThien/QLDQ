package com.qldq.qldqapi.service;

import com.qldq.qldqapi.config.BusinessTime;
import com.qldq.qldqapi.config.BusinessClock;
import com.qldq.qldqapi.entity.GuardAssignment;
import com.qldq.qldqapi.entity.GuardShift;
import com.qldq.qldqapi.entity.WorkItem;
import com.qldq.qldqapi.entity.WorkItemAssignment;
import com.qldq.qldqapi.entity.enums.GuardAssignmentStatus;
import com.qldq.qldqapi.entity.enums.GuardShiftStatus;
import com.qldq.qldqapi.entity.enums.NotificationReferenceType;
import com.qldq.qldqapi.entity.enums.ScheduleStatus;
import com.qldq.qldqapi.repository.GuardShiftRepository;
import com.qldq.qldqapi.repository.WorkItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NotificationReminderScheduler {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final GuardShiftRepository guardRepository;
    private final WorkItemRepository workItemRepository;
    private final NotificationService notificationService;
    private final BusinessClock businessClock;
    private final List<Integer> reminderHours;

    public NotificationReminderScheduler(
            GuardShiftRepository guardRepository,
            WorkItemRepository workItemRepository,
            NotificationService notificationService,
            BusinessClock businessClock,
            @Value("${app.notifications.reminder-hours:12,2}") List<Integer> reminderHours
    ) {
        this.guardRepository = guardRepository;
        this.workItemRepository = workItemRepository;
        this.notificationService = notificationService;
        this.businessClock = businessClock;
        this.reminderHours = reminderHours.stream().filter(value -> value > 0).distinct().sorted().toList();
    }

    @Scheduled(
            fixedDelayString = "${app.notifications.scheduler-delay-ms:60000}",
            initialDelayString = "${app.notifications.scheduler-delay-ms:60000}"
    )
    @Transactional
    public void sendDueReminders() {
        if (reminderHours.isEmpty()) return;
        ZonedDateTime now = businessClock.now();
        int maxHours = reminderHours.stream().mapToInt(Integer::intValue).max().orElse(12);
        sendGuardReminders(now, maxHours);
        sendWorkReminders(now, maxHours);
    }

    private void sendGuardReminders(ZonedDateTime now, int maxHours) {
        List<GuardShift> shifts = guardRepository
                .findAllByStatusAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
                        GuardShiftStatus.PUBLISHED, now.toOffsetDateTime(), now.plusHours(maxHours + 1).toOffsetDateTime());
        for (GuardShift shift : shifts) {
            ZonedDateTime start = shift.getStartAt().toInstant().atZone(BusinessTime.ZONE_ID);
            for (int hours : reminderHours) {
                if (now.isBefore(start.minusHours(hours)) || !now.isBefore(start)) continue;
                for (GuardAssignment assignment : shift.getAssignments()) {
                    if (assignment.getStatus() == GuardAssignmentStatus.COMPLETED) continue;
                    notificationService.reminder(assignment.getSoldier(), NotificationReferenceType.GUARD_SHIFT,
                            shift.getId(), hours, "Sắp đến giờ ca gác",
                            "Ca gác tại " + shift.getLocation() + " bắt đầu lúc " + FORMATTER.format(start)
                                    + " (còn khoảng " + hours + " giờ).");
                }
            }
        }
    }

    private void sendWorkReminders(ZonedDateTime now, int maxHours) {
        LocalDate from = now.toLocalDate();
        LocalDate to = now.plusHours(maxHours + 24L).toLocalDate();
        for (WorkItem item : workItemRepository.findReminderCandidates(ScheduleStatus.PUBLISHED, from, to)) {
            LocalTime startTime = item.isAllDay() || item.getStartTime() == null ? LocalTime.MIDNIGHT : item.getStartTime();
            ZonedDateTime start = item.getWorkDate().atTime(startTime).atZone(BusinessTime.ZONE_ID);
            for (int hours : reminderHours) {
                if (now.isBefore(start.minusHours(hours)) || !now.isBefore(start)) continue;
                for (WorkItemAssignment assignment : item.getAssignments()) {
                    notificationService.reminder(assignment.getSoldier(), NotificationService.workReferenceType(item),
                            item.getId(), hours, "Sắp đến giờ thực hiện nhiệm vụ",
                            item.getContent() + " bắt đầu lúc " + FORMATTER.format(start)
                                    + " (còn khoảng " + hours + " giờ).");
                }
            }
        }
    }
}
