package com.qldq.qldqapi.service;

import com.qldq.qldqapi.config.BusinessTime;
import com.qldq.qldqapi.dto.NotificationDtos.NotificationResponse;
import com.qldq.qldqapi.dto.NotificationDtos.UnreadCountResponse;
import com.qldq.qldqapi.dto.PagedResponse;
import com.qldq.qldqapi.entity.*;
import com.qldq.qldqapi.entity.enums.NotificationReferenceType;
import com.qldq.qldqapi.entity.enums.NotificationType;
import com.qldq.qldqapi.exception.NotFoundException;
import com.qldq.qldqapi.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HexFormat;

@Service
@Transactional
public class NotificationService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final NotificationRepository repository;
    private final UserAccessService accessService;

    public NotificationService(NotificationRepository repository, UserAccessService accessService) {
        this.repository = repository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> list(Long actorId, Pageable pageable) {
        accessService.requireActiveUser(actorId);
        Page<Notification> page = repository.findAllByRecipientId(actorId, pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long actorId) {
        accessService.requireActiveUser(actorId);
        return new UnreadCountResponse(repository.countByRecipientIdAndReadAtIsNull(actorId));
    }

    public NotificationResponse markRead(Long actorId, Long notificationId) {
        accessService.requireActiveUser(actorId);
        Notification notification = repository.findByIdAndRecipientId(notificationId, actorId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo"));
        if (notification.getReadAt() == null) notification.setReadAt(Instant.now());
        return toResponse(repository.save(notification));
    }

    public void markAllRead(Long actorId) {
        accessService.requireActiveUser(actorId);
        repository.markAllRead(actorId, Instant.now());
    }

    public void guardAssigned(GuardShift shift, Collection<UserAccount> recipients) {
        String when = format(shift);
        for (UserAccount recipient : recipients) {
            Long assignmentId = shift.getAssignments().stream()
                    .filter(assignment -> assignment.getSoldier().getId().equals(recipient.getId()))
                    .map(GuardAssignment::getId).findFirst().orElse(0L);
            create(recipient, "Bạn được phân công ca gác",
                    "Ca gác tại " + shift.getLocation() + " bắt đầu lúc " + when + ".",
                    NotificationType.GUARD_ASSIGNED, NotificationReferenceType.GUARD_SHIFT, shift.getId(),
                    "guard-assigned:" + shift.getId() + ":" + recipient.getId() + ":" + assignmentId);
        }
    }

    public void guardUpdated(GuardShift shift, Collection<UserAccount> recipients, String fingerprint) {
        String when = format(shift);
        for (UserAccount recipient : recipients) {
            create(recipient, "Ca gác đã được cập nhật",
                    "Ca gác liên quan đến bạn được cập nhật: " + when + ", " + shift.getLocation() + ".",
                    NotificationType.GUARD_UPDATED, NotificationReferenceType.GUARD_SHIFT, shift.getId(),
                    "guard-updated:" + shift.getId() + ":" + recipient.getId() + ":" + digest(fingerprint));
        }
    }

    public void guardCancelled(GuardShift shift) {
        for (GuardAssignment assignment : shift.getAssignments()) {
            UserAccount recipient = assignment.getSoldier();
            create(recipient, "Ca gác đã bị hủy",
                    "Ca gác lúc " + format(shift) + " đã bị Chỉ huy hủy.",
                    NotificationType.GUARD_CANCELLED, NotificationReferenceType.GUARD_SHIFT, shift.getId(),
                    "guard-cancelled:" + shift.getId() + ":" + recipient.getId());
        }
    }

    public void guardAcknowledged(GuardShift shift, UserAccount soldier) {
        Instant acknowledgedAt = shift.getAssignments().stream()
                .filter(assignment -> assignment.getSoldier().getId().equals(soldier.getId()))
                .map(GuardAssignment::getAcknowledgedAt).filter(java.util.Objects::nonNull)
                .findFirst().orElse(Instant.now());
        create(shift.getCreatedBy(), "Chiến sĩ đã xác nhận lịch",
                soldier.getFullName() + " đã xác nhận ca gác lúc " + format(shift) + ".",
                NotificationType.ACKNOWLEDGEMENT, NotificationReferenceType.GUARD_SHIFT, shift.getId(),
                "guard-acknowledged:" + shift.getId() + ":" + soldier.getId() + ":" + acknowledgedAt.toEpochMilli());
    }

    public void guardResultSubmitted(GuardShift shift, GuardAssignment assignment) {
        boolean incident = assignment.getResultCondition() != null
                && assignment.getResultCondition() != com.qldq.qldqapi.entity.enums.GuardResultCondition.NORMAL;
        create(shift.getCreatedBy(), incident ? "Ca gác có nội dung cần xem lại" : "Chiến sĩ đã gửi kết quả ca gác",
                assignment.getSoldier().getFullName() + " đã gửi kết quả ca gác lúc " + format(shift)
                        + (incident ? ": " + assignment.getIncidentNote() : "."),
                incident ? NotificationType.INCIDENT_REPORTED : NotificationType.RESULT_SUBMITTED,
                NotificationReferenceType.GUARD_SHIFT, shift.getId(),
                "guard-result:" + shift.getId() + ":" + assignment.getSoldier().getId());
    }

    public void workAssigned(WorkItem item, Collection<UserAccount> recipients) {
        NotificationReferenceType referenceType = workReferenceType(item);
        for (UserAccount recipient : recipients) {
            Long assignmentId = item.getAssignments().stream()
                    .filter(assignment -> assignment.getSoldier().getId().equals(recipient.getId()))
                    .map(WorkItemAssignment::getId).findFirst().orElse(0L);
            create(recipient, "Bạn được phân công công tác",
                    item.getContent() + " vào " + format(item) + locationSuffix(item) + ".",
                    NotificationType.WORK_ASSIGNED, referenceType, item.getId(),
                    "work-assigned:" + item.getId() + ":" + recipient.getId() + ":" + assignmentId);
        }
    }

    public void workUpdated(WorkItem item, Collection<UserAccount> recipients, String fingerprint) {
        NotificationReferenceType referenceType = workReferenceType(item);
        for (UserAccount recipient : recipients) {
            create(recipient, "Nhiệm vụ công tác đã thay đổi",
                    item.getContent() + " vào " + format(item) + locationSuffix(item) + ".",
                    NotificationType.WORK_UPDATED, referenceType, item.getId(),
                    "work-updated:" + item.getId() + ":" + recipient.getId() + ":" + digest(fingerprint));
        }
    }

    public void workRemoved(Long itemId, String content, NotificationReferenceType referenceType,
                            Collection<UserAccount> recipients) {
        for (UserAccount recipient : recipients) {
            create(recipient, "Nhiệm vụ đã được gỡ khỏi lịch",
                    "Nhiệm vụ “" + content + "” không còn trong lịch công tác.",
                    NotificationType.WORK_UPDATED, referenceType, itemId,
                    "work-removed:" + itemId + ":" + recipient.getId());
        }
    }

    public void reminder(UserAccount recipient, NotificationReferenceType referenceType, Long referenceId,
                         int hoursBefore, String title, String content) {
        create(recipient, title, content, NotificationType.REMINDER, referenceType, referenceId,
                "reminder:" + referenceType + ":" + referenceId + ":" + recipient.getId() + ":" + hoursBefore + "h");
    }

    private void create(UserAccount recipient, String title, String content, NotificationType type,
                        NotificationReferenceType referenceType, Long referenceId, String eventKey) {
        if (repository.existsByEventKey(eventKey)) return;
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setEventKey(eventKey);
        repository.save(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getContent(),
                notification.getType(), notification.getReferenceType(), notification.getReferenceId(),
                notification.getCreatedAt(), notification.getReadAt());
    }

    public static String guardFingerprint(GuardShift shift) {
        return shift.getStartAt() + "|" + shift.getEndAt() + "|" + shift.getLocation() + "|" + shift.getNote();
    }

    public static String workFingerprint(WorkItem item) {
        return item.getWorkDate() + "|" + item.getStartTime() + "|" + item.getEndTime() + "|"
                + item.getContent() + "|" + item.getLocation() + "|" + item.getNote();
    }

    private String format(GuardShift shift) {
        return DATE_TIME_FORMAT.format(shift.getStartAt().toInstant().atZone(BusinessTime.ZONE_ID));
    }

    private String format(WorkItem item) {
        if (item.isAllDay() || item.getStartTime() == null) return "ngày " + item.getWorkDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return item.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " ngày "
                + item.getWorkDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String locationSuffix(WorkItem item) {
        return item.getLocation() == null || item.getLocation().isBlank() ? "" : " tại " + item.getLocation();
    }

    public static NotificationReferenceType workReferenceType(WorkItem item) {
        return item.getWorkSchedule().getScheduleType() == com.qldq.qldqapi.entity.enums.ScheduleType.AREA_MONITORING
                ? NotificationReferenceType.AREA_MONITORING_ITEM : NotificationReferenceType.WORK_ITEM;
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
