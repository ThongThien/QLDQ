package com.qldq.qldqapi.service;

import com.qldq.qldqapi.config.BusinessClock;
import com.qldq.qldqapi.dto.GuardDtos.SoldierBrief;
import com.qldq.qldqapi.dto.PagedResponse;
import com.qldq.qldqapi.dto.WorkScheduleDtos.*;
import com.qldq.qldqapi.entity.*;
import com.qldq.qldqapi.entity.enums.*;
import com.qldq.qldqapi.exception.BusinessException;
import com.qldq.qldqapi.exception.ConflictException;
import com.qldq.qldqapi.exception.ForbiddenException;
import com.qldq.qldqapi.exception.NotFoundException;
import com.qldq.qldqapi.repository.UserAccountRepository;
import com.qldq.qldqapi.repository.NotificationRepository;
import com.qldq.qldqapi.repository.WorkItemRepository;
import com.qldq.qldqapi.repository.WorkScheduleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkScheduleService {
    private final WorkScheduleRepository scheduleRepository;
    private final WorkItemRepository itemRepository;
    private final UserAccountRepository userRepository;
    private final UserAccessService accessService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final BusinessClock businessClock;

    public WorkScheduleService(
            WorkScheduleRepository scheduleRepository,
            WorkItemRepository itemRepository,
            UserAccountRepository userRepository,
            UserAccessService accessService,
            NotificationService notificationService,
            NotificationRepository notificationRepository,
            ActivityLogService activityLogService,
            BusinessClock businessClock
    ) {
        this.scheduleRepository = scheduleRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.businessClock = businessClock;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ScheduleResponse> list(Long actorId, ScheduleType type, boolean mine, Pageable pageable) {
        UserAccount actor = accessService.requireActiveUser(actorId);
        boolean soldier = actor.getRole() == UserRole.SOLDIER;
        Page<WorkSchedule> page = soldier && mine
                ? scheduleRepository.findAssignedPublished(type, ScheduleStatus.PUBLISHED, actorId, pageable)
                : soldier
                ? scheduleRepository.findAllByScheduleTypeAndStatus(type, ScheduleStatus.PUBLISHED, pageable)
                : scheduleRepository.findAllByScheduleType(type, pageable);
        List<ScheduleResponse> content = page.getContent().stream()
                .map(schedule -> toScheduleResponse(schedule, soldier && mine ? actorId : null)).toList();
        return PagedResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse get(Long actorId, ScheduleType type, Long scheduleId) {
        UserAccount actor = accessService.requireActiveUser(actorId);
        WorkSchedule schedule = getSchedule(type, scheduleId);
        if (actor.getRole() == UserRole.SOLDIER) {
            if (schedule.getStatus() != ScheduleStatus.PUBLISHED) {
                throw new ForbiddenException("Chiến sĩ chỉ được xem lịch đã công bố");
            }
            return toScheduleResponse(schedule);
        }
        return toScheduleResponse(schedule);
    }

    public ScheduleResponse create(Long actorId, ScheduleType type, ScheduleRequest request) {
        UserAccount commander = accessService.requireCommander(actorId);
        validateWeek(request.weekStartDate());
        if (scheduleRepository.findByScheduleTypeAndWeekStartDate(type, request.weekStartDate()).isPresent()) {
            throw new ConflictException("Đã tồn tại lịch cùng loại trong tuần này");
        }
        WorkSchedule schedule = new WorkSchedule();
        apply(schedule, type, request);
        schedule.setCreatedBy(commander);
        WorkSchedule saved = scheduleRepository.save(schedule);
        activityLogService.record(commander, ActivityAction.WORK_SCHEDULE_CREATED, ActivityTargetType.WORK_SCHEDULE,
                saved.getId(), "Đã tạo " + scheduleLabel(type) + " “" + saved.getTitle() + "”.");
        return toScheduleResponse(saved);
    }

    public ScheduleResponse update(Long actorId, ScheduleType type, Long scheduleId, ScheduleRequest request) {
        UserAccount commander = accessService.requireCommander(actorId);
        validateWeek(request.weekStartDate());
        WorkSchedule schedule = getSchedule(type, scheduleId);
        String previousFingerprint = scheduleFingerprint(schedule);
        if (schedule.getStatus() == ScheduleStatus.PUBLISHED && request.status() == ScheduleStatus.DRAFT) {
            throw new ConflictException("Không thể chuyển lịch đã công bố trở lại trạng thái nháp");
        }
        scheduleRepository.findByScheduleTypeAndWeekStartDate(type, request.weekStartDate())
                .filter(existing -> !existing.getId().equals(scheduleId))
                .ifPresent(existing -> {
                    throw new ConflictException("Đã tồn tại lịch cùng loại trong tuần này");
                });
        LocalDate newEnd = request.weekStartDate().plusDays(6);
        boolean itemOutside = schedule.getItems().stream().anyMatch(item ->
                item.getWorkDate().isBefore(request.weekStartDate()) || item.getWorkDate().isAfter(newEnd));
        if (itemOutside) {
            throw new ConflictException("Không thể đổi tuần vì có công việc nằm ngoài tuần mới");
        }
        apply(schedule, type, request);
        WorkSchedule saved = scheduleRepository.save(schedule);
        if (saved.getStatus() == ScheduleStatus.PUBLISHED
                && !previousFingerprint.equals(scheduleFingerprint(saved))) {
            for (WorkItem item : saved.getItems()) {
                notificationService.workUpdated(item, assignedUsers(item),
                        NotificationService.workFingerprint(item) + "|" + scheduleFingerprint(saved));
            }
        }
        if (!previousFingerprint.equals(scheduleFingerprint(saved))) {
            activityLogService.record(commander, ActivityAction.WORK_SCHEDULE_UPDATED,
                    ActivityTargetType.WORK_SCHEDULE, saved.getId(), "Đã chỉnh sửa " + scheduleLabel(type) + ".");
        }
        return toScheduleResponse(saved);
    }

    public ScheduleResponse publish(Long actorId, ScheduleType type, Long scheduleId) {
        UserAccount commander = accessService.requireCommander(actorId);
        WorkSchedule schedule = getSchedule(type, scheduleId);
        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            return toScheduleResponse(schedule);
        }
        if (schedule.getItems().isEmpty()) {
            throw new BusinessException("Không thể công bố lịch chưa có công việc");
        }
        schedule.setStatus(ScheduleStatus.PUBLISHED);
        WorkSchedule saved = scheduleRepository.save(schedule);
        for (WorkItem item : saved.getItems()) {
            notificationService.workAssigned(item, assignedUsers(item));
        }
        activityLogService.record(commander, ActivityAction.WORK_SCHEDULE_PUBLISHED,
                ActivityTargetType.WORK_SCHEDULE, saved.getId(), "Đã công bố " + scheduleLabel(type) + ".");
        return toScheduleResponse(saved);
    }

    public void delete(Long actorId, ScheduleType type, Long scheduleId) {
        accessService.requireCommander(actorId);
        WorkSchedule schedule = getSchedule(type, scheduleId);
        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new ConflictException("Chỉ được xóa lịch đang ở trạng thái nháp");
        }
        scheduleRepository.delete(schedule);
    }

    public WorkItemResponse addItem(Long actorId, ScheduleType type, Long scheduleId, WorkItemRequest request) {
        UserAccount commander = accessService.requireCommander(actorId);
        WorkSchedule schedule = getSchedule(type, scheduleId);
        validateItem(schedule, request);
        WorkItem item = new WorkItem();
        item.setWorkSchedule(schedule);
        apply(item, request);
        replaceAssignments(item, request.soldierIds());
        schedule.getItems().add(item);
        WorkItem saved = itemRepository.save(item);
        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            notificationService.workAssigned(saved, assignedUsers(saved));
        }
        activityLogService.record(commander, ActivityAction.WORK_ITEM_CREATED, ActivityTargetType.WORK_ITEM,
                saved.getId(), "Đã thêm nhiệm vụ vào " + scheduleLabel(type) + " ngày " + saved.getWorkDate() + ".");
        return toItemResponse(saved);
    }

    public WorkItemResponse updateItem(
            Long actorId, ScheduleType type, Long scheduleId, Long itemId, WorkItemRequest request
    ) {
        UserAccount commander = accessService.requireCommander(actorId);
        WorkSchedule schedule = getSchedule(type, scheduleId);
        WorkItem item = getItem(schedule, itemId);
        String previousFingerprint = NotificationService.workFingerprint(item);
        Map<Long, UserAccount> previousAssignees = assignedUsers(item).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        validateItem(schedule, request);
        apply(item, request);
        replaceAssignments(item, request.soldierIds());
        WorkItem saved = itemRepository.save(item);
        Map<Long, UserAccount> currentAssignees = assignedUsers(saved).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        boolean itemChanged = !previousFingerprint.equals(NotificationService.workFingerprint(saved))
                || !previousAssignees.keySet().equals(currentAssignees.keySet());
        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            List<UserAccount> added = currentAssignees.entrySet().stream()
                    .filter(entry -> !previousAssignees.containsKey(entry.getKey())).map(Map.Entry::getValue).toList();
            notificationService.workAssigned(saved, added);
            Set<UserAccount> changedRecipients = new LinkedHashSet<>();
            if (!previousFingerprint.equals(NotificationService.workFingerprint(saved))) {
                changedRecipients.addAll(previousAssignees.values());
                changedRecipients.addAll(currentAssignees.values());
            } else {
                previousAssignees.forEach((id, user) -> {
                    if (!currentAssignees.containsKey(id)) changedRecipients.add(user);
                });
            }
            String assignmentFingerprint = NotificationService.workFingerprint(saved) + "|" +
                    currentAssignees.keySet().stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
            notificationService.workUpdated(saved, changedRecipients, assignmentFingerprint);
        }
        if (itemChanged) {
            activityLogService.record(commander, ActivityAction.WORK_ITEM_UPDATED, ActivityTargetType.WORK_ITEM,
                    saved.getId(), "Đã chỉnh sửa nhiệm vụ trong " + scheduleLabel(type) + " ngày " + saved.getWorkDate() + ".");
        }
        return toItemResponse(saved);
    }

    public void deleteItem(Long actorId, ScheduleType type, Long scheduleId, Long itemId) {
        UserAccount commander = accessService.requireCommander(actorId);
        WorkSchedule schedule = getSchedule(type, scheduleId);
        WorkItem item = getItem(schedule, itemId);
        if (schedule.getStatus() == ScheduleStatus.PUBLISHED) {
            notificationService.workRemoved(item.getId(), item.getContent(),
                    NotificationService.workReferenceType(item), assignedUsers(item));
        }
        activityLogService.record(commander, ActivityAction.WORK_ITEM_REMOVED, ActivityTargetType.WORK_ITEM,
                item.getId(), "Đã xóa nhiệm vụ khỏi " + scheduleLabel(type) + " ngày " + item.getWorkDate() + ".");
        schedule.getItems().remove(item);
        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public PagedResponse<WorkItemResponse> searchAreaItems(
            Long actorId, LocalDate date, WorkPeriod period, boolean mine, Pageable pageable
    ) {
        UserAccount actor = accessService.requireActiveUser(actorId);
        Long soldierId = actor.getRole() == UserRole.SOLDIER && mine ? actorId : null;
        Page<WorkItem> page = itemRepository.searchByScheduleType(
                ScheduleType.AREA_MONITORING,
                actor.getRole() == UserRole.SOLDIER ? ScheduleStatus.PUBLISHED : null,
                date, period, soldierId, pageable);
        List<WorkItemResponse> content = page.getContent().stream().map(this::toItemResponse).toList();
        return PagedResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public WorkItemResponse getAssignedItem(Long actorId, Long itemId) {
        UserAccount actor = accessService.requireActiveUser(actorId);
        WorkItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy công việc"));
        if (actor.getRole() == UserRole.SOLDIER) {
            if (item.getWorkSchedule().getStatus() != ScheduleStatus.PUBLISHED) {
                throw new ForbiddenException("Chiến sĩ chỉ được xem công việc thuộc lịch đã công bố");
            }
        }
        return toItemResponse(item);
    }

    private void validateWeek(LocalDate weekStartDate) {
        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BusinessException("Ngày bắt đầu tuần phải là thứ Hai");
        }
    }

    private void validateItem(WorkSchedule schedule, WorkItemRequest request) {
        if (request.workDate().isBefore(schedule.getWeekStartDate())
                || request.workDate().isAfter(schedule.getWeekEndDate())) {
            throw new BusinessException("Ngày công việc phải thuộc tuần của lịch");
        }
        if (request.allDay()) {
            if (request.startTime() != null || request.endTime() != null) {
                throw new BusinessException("Công việc cả ngày không được có giờ bắt đầu hoặc kết thúc");
            }
        } else {
            if (request.startTime() == null || request.endTime() == null) {
                throw new BusinessException("Công việc theo giờ phải có giờ bắt đầu và kết thúc");
            }
            if (request.endTime().equals(request.startTime())) {
                throw new BusinessException("Giờ kết thúc không được trùng giờ bắt đầu");
            }
        }
        List<Long> ids = request.soldierIds() == null ? List.of() : request.soldierIds();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException("Danh sách chiến sĩ có thành viên bị trùng");
        }
    }

    private void apply(WorkSchedule schedule, ScheduleType type, ScheduleRequest request) {
        schedule.setScheduleType(type);
        schedule.setTitle(request.title().trim());
        schedule.setWeekStartDate(request.weekStartDate());
        schedule.setWeekEndDate(request.weekStartDate().plusDays(6));
        schedule.setScopeArea(blankToNull(request.scopeArea()));
        schedule.setGeneralNote(blankToNull(request.generalNote()));
        schedule.setStatus(request.status());
    }

    private void apply(WorkItem item, WorkItemRequest request) {
        item.setWorkDate(request.workDate());
        item.setPeriod(request.allDay() ? WorkPeriod.ALL_DAY : request.period());
        item.setStartTime(request.startTime());
        item.setEndTime(request.endTime());
        item.setAllDay(request.allDay());
        item.setContent(request.content().trim());
        item.setLeader(blankToNull(request.leader()));
        item.setParticipantsDescription(blankToNull(request.participantsDescription()));
        item.setLocation(blankToNull(request.location()));
        item.setPreparation(blankToNull(request.preparation()));
        item.setDutyLeader(blankToNull(request.dutyLeader()));
        item.setNote(blankToNull(request.note()));
        item.setSortOrder(request.sortOrder());
    }

    private void replaceAssignments(WorkItem item, List<Long> requestedIds) {
        List<Long> soldierIds = requestedIds == null ? List.of() : requestedIds;
        List<UserAccount> users = userRepository.findAllById(new LinkedHashSet<>(soldierIds));
        if (users.size() != soldierIds.size()) {
            throw new NotFoundException("Có chiến sĩ được phân công không tồn tại");
        }
        Map<Long, UserAccount> byId = users.stream().collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        for (Long soldierId : soldierIds) {
            UserAccount soldier = byId.get(soldierId);
            if (soldier.getRole() != UserRole.SOLDIER) {
                throw new BusinessException("Chỉ tài khoản chiến sĩ mới được phân công");
            }
            if (!soldier.isActive()) {
                throw new BusinessException("Không thể phân công chiến sĩ đã bị vô hiệu hóa: " + soldier.getFullName());
            }
        }
        Set<Long> desired = new LinkedHashSet<>(soldierIds);
        item.getAssignments().removeIf(assignment -> !desired.contains(assignment.getSoldier().getId()));
        Set<Long> existing = item.getAssignments().stream()
                .map(assignment -> assignment.getSoldier().getId()).collect(Collectors.toSet());
        for (Long soldierId : soldierIds) {
            if (!existing.contains(soldierId)) {
                WorkItemAssignment assignment = new WorkItemAssignment();
                assignment.setWorkItem(item);
                assignment.setSoldier(byId.get(soldierId));
                item.getAssignments().add(assignment);
            }
        }
    }

    private WorkSchedule getSchedule(ScheduleType type, Long scheduleId) {
        WorkSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lịch công tác"));
        if (schedule.getScheduleType() != type) {
            throw new NotFoundException("Lịch không thuộc nhóm nghiệp vụ được yêu cầu");
        }
        return schedule;
    }

    private WorkItem getItem(WorkSchedule schedule, Long itemId) {
        return schedule.getItems().stream().filter(item -> item.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy công việc trong lịch"));
    }

    private List<UserAccount> assignedUsers(WorkItem item) {
        return item.getAssignments().stream().map(WorkItemAssignment::getSoldier).toList();
    }

    private String scheduleFingerprint(WorkSchedule schedule) {
        return schedule.getTitle() + "|" + schedule.getWeekStartDate() + "|" + schedule.getWeekEndDate()
                + "|" + schedule.getScopeArea() + "|" + schedule.getGeneralNote();
    }

    private String scheduleLabel(ScheduleType type) {
        return type == ScheduleType.AREA_MONITORING ? "lịch theo dõi địa bàn" : "lịch công tác tuần";
    }

    private ScheduleResponse toScheduleResponse(WorkSchedule schedule) {
        return toScheduleResponse(schedule, null);
    }

    private ScheduleResponse toScheduleResponse(WorkSchedule schedule, Long soldierId) {
        List<WorkItemResponse> items = schedule.getItems().stream()
                .filter(item -> soldierId == null || item.getAssignments().stream()
                        .anyMatch(assignment -> assignment.getSoldier().getId().equals(soldierId)))
                .sorted(Comparator.comparing(WorkItem::getWorkDate).thenComparingInt(WorkItem::getSortOrder))
                .map(this::toItemResponse)
                .toList();
        int weekNumber = schedule.getWeekStartDate().get(WeekFields.ISO.weekOfWeekBasedYear());
        String weekLabel = "Tuần " + weekNumber + " năm " + schedule.getWeekStartDate().getYear();
        return new ScheduleResponse(
                schedule.getId(), schedule.getScheduleType(), schedule.getTitle(), schedule.getWeekStartDate(),
                schedule.getWeekEndDate(), weekLabel, schedule.getScopeArea(), schedule.getGeneralNote(),
                schedule.getStatus(), toBrief(schedule.getCreatedBy()), items,
                schedule.getCreatedAt(), schedule.getUpdatedAt()
        );
    }

    private WorkItemResponse toItemResponse(WorkItem item) {
        List<SoldierBrief> soldiers = item.getAssignments().stream()
                .map(WorkItemAssignment::getSoldier)
                .sorted(Comparator.comparing(UserAccount::getFullName))
                .map(this::toBrief)
                .toList();
        return new WorkItemResponse(
                item.getId(), item.getWorkSchedule().getId(), item.getWorkDate(), item.getPeriod(),
                item.getStartTime(), item.getEndTime(), item.isAllDay(), item.getContent(), item.getLeader(),
                item.getParticipantsDescription(), item.getLocation(), item.getPreparation(), item.getDutyLeader(),
                item.getNote(), item.getSortOrder(), occurrenceStatus(item), soldiers,
                item.getCreatedAt(), item.getUpdatedAt()
        );
    }

    private OccurrenceStatus occurrenceStatus(WorkItem item) {
        return item.getOccurrenceStatus(businessClock.instant());
    }

    private SoldierBrief toBrief(UserAccount user) {
        return new SoldierBrief(user.getId(), user.getUsername(), user.getFullName(), user.getRankOrPosition(), user.isActive());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
