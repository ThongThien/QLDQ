package com.qldq.qldqapi.service;

import com.qldq.qldqapi.config.BusinessTime;
import com.qldq.qldqapi.config.BusinessClock;
import com.qldq.qldqapi.dto.GuardDtos.*;
import com.qldq.qldqapi.dto.PagedResponse;
import com.qldq.qldqapi.entity.GuardAssignment;
import com.qldq.qldqapi.entity.GuardShift;
import com.qldq.qldqapi.entity.UserAccount;
import com.qldq.qldqapi.entity.enums.*;
import com.qldq.qldqapi.exception.BusinessException;
import com.qldq.qldqapi.exception.ConflictException;
import com.qldq.qldqapi.exception.ForbiddenException;
import com.qldq.qldqapi.exception.NotFoundException;
import com.qldq.qldqapi.repository.GuardShiftRepository;
import com.qldq.qldqapi.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class GuardShiftService {
    private static final String MAIN_GATE = "Cổng chính";
    private static final List<LocalTime> WEEKLY_SHIFT_STARTS = List.of(
            LocalTime.of(7, 0), LocalTime.of(9, 0), LocalTime.of(11, 0),
            LocalTime.of(13, 0), LocalTime.of(15, 0), LocalTime.of(17, 0),
            LocalTime.of(19, 0), LocalTime.of(21, 0), LocalTime.of(23, 0),
            LocalTime.of(1, 0), LocalTime.of(3, 0), LocalTime.of(5, 0)
    );
    private final GuardShiftRepository shiftRepository;
    private final UserAccountRepository userRepository;
    private final UserAccessService accessService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final BusinessClock businessClock;

    public GuardShiftService(
            GuardShiftRepository shiftRepository,
            UserAccountRepository userRepository,
            UserAccessService accessService,
            NotificationService notificationService,
            ActivityLogService activityLogService,
            BusinessClock businessClock
    ) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.businessClock = businessClock;
    }

    @Transactional(readOnly = true)
    public PagedResponse<GuardShiftResponse> search(
            Long actorId,
            LocalDate from,
            LocalDate to,
            GuardShiftStatus status,
            Long soldierId,
            Pageable pageable
    ) {
        UserAccount actor = accessService.requireActiveUser(actorId);
        if (actor.getRole() == UserRole.SOLDIER) {
            if (status == GuardShiftStatus.DRAFT) {
                throw new ForbiddenException("Chiến sĩ không được xem ca gác nháp");
            }
            if (soldierId != null && !soldierId.equals(actor.getId())) {
                throw new ForbiddenException("Chiến sĩ chỉ được lọc lịch cá nhân của mình");
            }
        }
        LocalDate startDate = from != null ? from : businessClock.today().with(DayOfWeek.MONDAY);
        LocalDate endDate = to != null ? to : startDate.plusDays(6);
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("Ngày kết thúc bộ lọc phải từ ngày bắt đầu trở đi");
        }
        OffsetDateTime fromAt = startDate.atStartOfDay(BusinessTime.ZONE_ID).toOffsetDateTime();
        OffsetDateTime toExclusive = endDate.plusDays(1).atStartOfDay(BusinessTime.ZONE_ID).toOffsetDateTime();
        GuardShiftStatus effectiveStatus = actor.getRole() == UserRole.SOLDIER && status == null
                ? GuardShiftStatus.PUBLISHED : status;
        Page<GuardShift> page = shiftRepository.searchOverlapping(fromAt, toExclusive, effectiveStatus, soldierId, pageable);
        List<GuardShiftResponse> content = page.getContent().stream()
                .map(shift -> toResponse(shift, actor))
                .toList();
        return PagedResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public GuardShiftResponse get(Long actorId, Long shiftId) {
        UserAccount actor = accessService.requireActiveUser(actorId);
        GuardShift shift = getShift(shiftId);
        if (actor.getRole() == UserRole.SOLDIER && shift.getStatus() == GuardShiftStatus.DRAFT) {
            throw new ForbiddenException("Chiến sĩ không được xem ca gác nháp");
        }
        return toResponse(shift, actor);
    }

    public GuardShiftResponse create(Long actorId, GuardShiftRequest request) {
        UserAccount commander = accessService.requireCommander(actorId);
        validateRequest(request);
        GuardShift shift = new GuardShift();
        apply(shift, request);
        shift.setCreatedBy(commander);
        replaceAssignments(shift, request.soldierIds());
        GuardShift saved = shiftRepository.save(shift);
        if (saved.getStatus() == GuardShiftStatus.PUBLISHED) {
            notificationService.guardAssigned(saved, assignedUsers(saved));
        }
        activityLogService.record(commander, ActivityAction.GUARD_CREATED, ActivityTargetType.GUARD_SHIFT,
                saved.getId(), "Đã tạo ca gác tại " + saved.getLocation() + ".");
        return toResponse(saved, commander);
    }

    public GuardWeekResponse generateWeek(Long actorId, GuardWeekRequest request) {
        UserAccount commander = accessService.requireCommander(actorId);
        LocalDate weekStart = request.weekStartDate();
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BusinessException("Ngày bắt đầu tuần phải là Thứ Hai");
        }

        OffsetDateTime from = weekStart.atStartOfDay(BusinessTime.ZONE_ID).toOffsetDateTime();
        OffsetDateTime toExclusive = weekStart.plusDays(7).atStartOfDay(BusinessTime.ZONE_ID).toOffsetDateTime();
        if (shiftRepository.countByStartAtGreaterThanEqualAndStartAtLessThan(from, toExclusive) > 0) {
            throw new ConflictException("Tuần đã chọn đã có lịch gác; không thể tạo trùng tuần");
        }

        List<UserAccount> soldiers = new ArrayList<>(
                userRepository.findAllByRoleAndActiveTrueOrderByFullName(UserRole.SOLDIER));
        if (soldiers.size() < 2) {
            throw new BusinessException("Cần ít nhất 2 chiến sĩ đang hoạt động để tạo lịch gác tuần");
        }
        Collections.shuffle(soldiers);

        List<GuardShift> shifts = new ArrayList<>(84);
        int assignmentCursor = 0;
        for (int day = 0; day < 7; day++) {
            LocalDate shiftDate = weekStart.plusDays(day);
            for (LocalTime startTime : WEEKLY_SHIFT_STARTS) {
                ZonedDateTime localStart = shiftDate.atTime(startTime).atZone(BusinessTime.ZONE_ID);
                GuardShift shift = new GuardShift();
                shift.setStartAt(localStart.toOffsetDateTime());
                shift.setEndAt(localStart.plusHours(2).toOffsetDateTime());
                shift.setLocation(MAIN_GATE);
                shift.setNote("Thực hiện đúng quy định gác cổng; bàn giao đầy đủ khi hết ca.");
                shift.setStatus(GuardShiftStatus.PUBLISHED);
                shift.setCreatedBy(commander);

                UserAccount first = soldiers.get(assignmentCursor % soldiers.size());
                UserAccount second = soldiers.get((assignmentCursor + 1) % soldiers.size());
                replaceAssignments(shift, List.of(first.getId(), second.getId()));
                assignmentCursor = (assignmentCursor + 2) % soldiers.size();
                shifts.add(shift);
            }
        }

        List<GuardShift> saved = shiftRepository.saveAll(shifts);
        saved.forEach(shift -> notificationService.guardAssigned(shift, assignedUsers(shift)));
        activityLogService.record(commander, ActivityAction.GUARD_CREATED, ActivityTargetType.GUARD_SHIFT,
                saved.getFirst().getId(), "Đã tạo lịch gác tuần " + weekStart + " đến " + weekStart.plusDays(6)
                        + " gồm 84 ca và phân công ngẫu nhiên chiến sĩ.");
        return new GuardWeekResponse(weekStart, weekStart.plusDays(6), saved.size(), saved.size() * 2);
    }

    public GuardShiftResponse update(Long actorId, Long shiftId, GuardShiftRequest request) {
        UserAccount commander = accessService.requireCommander(actorId);
        validateRequest(request);
        GuardShift shift = getShift(shiftId);
        requireEditable(shift);
        GuardShiftStatus previousStatus = shift.getStatus();
        String previousFingerprint = NotificationService.guardFingerprint(shift);
        Map<Long, UserAccount> previousAssignees = assignedUsers(shift).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        if (shift.getStatus() == GuardShiftStatus.PUBLISHED && request.status() == GuardShiftStatus.DRAFT) {
            throw new ConflictException("Không thể chuyển ca đã công bố trở lại trạng thái nháp");
        }
        apply(shift, request);
        replaceAssignments(shift, request.soldierIds());
        boolean detailsChanged = !previousFingerprint.equals(NotificationService.guardFingerprint(shift));
        if (previousStatus == GuardShiftStatus.PUBLISHED && detailsChanged) {
            shift.getAssignments().stream()
                    .filter(assignment -> previousAssignees.containsKey(assignment.getSoldier().getId()))
                    .forEach(assignment -> assignment.setAcknowledgedAt(null));
        }
        GuardShift saved = shiftRepository.save(shift);
        Map<Long, UserAccount> currentAssignees = assignedUsers(saved).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        if (detailsChanged) {
            activityLogService.record(commander, ActivityAction.GUARD_SCHEDULE_CHANGED,
                    ActivityTargetType.GUARD_SHIFT, saved.getId(), "Đã thay đổi thời gian, địa điểm hoặc nội dung ca gác.");
        }
        Set<Long> addedIds = new LinkedHashSet<>(currentAssignees.keySet());
        addedIds.removeAll(previousAssignees.keySet());
        Set<Long> removedIds = new LinkedHashSet<>(previousAssignees.keySet());
        removedIds.removeAll(currentAssignees.keySet());
        if (!addedIds.isEmpty() || !removedIds.isEmpty()) {
            String addedNames = addedIds.stream().map(currentAssignees::get).map(UserAccount::getFullName).collect(Collectors.joining(", "));
            String removedNames = removedIds.stream().map(previousAssignees::get).map(UserAccount::getFullName).collect(Collectors.joining(", "));
            String description = (!addedNames.isBlank() ? "Thêm: " + addedNames + ". " : "")
                    + (!removedNames.isBlank() ? "Xóa: " + removedNames + "." : "");
            activityLogService.record(commander, ActivityAction.GUARD_ASSIGNMENTS_CHANGED,
                    ActivityTargetType.GUARD_SHIFT, saved.getId(), description.trim());
        }
        if (saved.getStatus() == GuardShiftStatus.PUBLISHED) {
            if (previousStatus != GuardShiftStatus.PUBLISHED) {
                notificationService.guardAssigned(saved, currentAssignees.values());
            } else {
                List<UserAccount> added = currentAssignees.entrySet().stream()
                        .filter(entry -> !previousAssignees.containsKey(entry.getKey())).map(Map.Entry::getValue).toList();
                notificationService.guardAssigned(saved, added);
                Set<UserAccount> changedRecipients = new LinkedHashSet<>();
                if (detailsChanged) {
                    changedRecipients.addAll(previousAssignees.values());
                    changedRecipients.addAll(currentAssignees.values());
                } else {
                    previousAssignees.forEach((id, user) -> {
                        if (!currentAssignees.containsKey(id)) changedRecipients.add(user);
                    });
                }
                String assignmentFingerprint = NotificationService.guardFingerprint(saved) + "|" +
                        currentAssignees.keySet().stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
                notificationService.guardUpdated(saved, changedRecipients, assignmentFingerprint);
            }
        }
        return toResponse(saved, commander);
    }

    public GuardShiftResponse changeStatus(Long actorId, Long shiftId, GuardStatusRequest request) {
        UserAccount commander = accessService.requireCommander(actorId);
        GuardShift shift = getShift(shiftId);
        GuardShiftStatus target = request.status();
        if (target == GuardShiftStatus.DRAFT) {
            throw new BusinessException("Không thể chuyển ca gác trở lại trạng thái nháp");
        }
        if (target == GuardShiftStatus.PUBLISHED) {
            if (shift.getStatus() != GuardShiftStatus.DRAFT) {
                throw new ConflictException("Chỉ ca gác nháp mới có thể công bố");
            }
            if (shift.getAssignments().isEmpty()) {
                throw new BusinessException("Ca gác phải có ít nhất một chiến sĩ trước khi công bố");
            }
        } else if (target == GuardShiftStatus.CANCELLED) {
            if (shift.getStatus() == GuardShiftStatus.CANCELLED) {
                return toResponse(shift, commander);
            }
            if (shift.getOccurrenceStatus(businessClock.instant()) == OccurrenceStatus.COMPLETED) {
                throw new ConflictException("Không thể hủy ca gác đã kết thúc");
            }
        }
        shift.setStatus(target);
        GuardShift saved = shiftRepository.save(shift);
        if (target == GuardShiftStatus.PUBLISHED) notificationService.guardAssigned(saved, assignedUsers(saved));
        if (target == GuardShiftStatus.CANCELLED) {
            notificationService.guardCancelled(saved);
            activityLogService.record(commander, ActivityAction.GUARD_CANCELLED, ActivityTargetType.GUARD_SHIFT,
                    saved.getId(), "Đã hủy ca gác tại " + saved.getLocation() + ".");
        }
        return toResponse(saved, commander);
    }

    public void delete(Long actorId, Long shiftId) {
        accessService.requireCommander(actorId);
        GuardShift shift = getShift(shiftId);
        if (shift.getStatus() != GuardShiftStatus.DRAFT) {
            throw new ConflictException("Chỉ được xóa ca gác nháp; ca đã công bố phải được hủy");
        }
        shiftRepository.delete(shift);
    }

    public GuardShiftResponse acknowledge(Long actorId, Long shiftId) {
        UserAccount soldier = accessService.requireSoldier(actorId);
        GuardShift shift = getShift(shiftId);
        if (shift.getStatus() != GuardShiftStatus.PUBLISHED) {
            throw new ConflictException("Chỉ có thể xác nhận ca gác đã được công bố");
        }
        GuardAssignment assignment = requireAssignment(shift, soldier.getId());
        if (assignment.getAcknowledgedAt() == null) {
            assignment.setAcknowledgedAt(Instant.now());
            notificationService.guardAcknowledged(shift, soldier);
            activityLogService.record(soldier, ActivityAction.GUARD_ACKNOWLEDGED, ActivityTargetType.GUARD_SHIFT,
                    shift.getId(), "Đã xác nhận tiếp nhận ca gác.");
        }
        return toResponse(shiftRepository.save(shift), soldier);
    }

    public GuardShiftResponse complete(Long actorId, Long shiftId, GuardResultRequest request) {
        UserAccount soldier = accessService.requireSoldier(actorId);
        GuardShift shift = getShift(shiftId);
        if (shift.getStatus() != GuardShiftStatus.PUBLISHED) {
            throw new ConflictException("Không thể ghi nhận kết quả cho ca chưa công bố hoặc đã hủy");
        }
        if (businessClock.instant().isBefore(shift.getEndAt().toInstant())) {
            throw new ConflictException("Chỉ có thể ghi nhận kết quả sau khi ca gác kết thúc");
        }
        if (request.resultCondition() != GuardResultCondition.NORMAL
                && (request.incidentNote() == null || request.incidentNote().isBlank())) {
            throw new BusinessException("Phải nhập nội dung sự cố hoặc nội dung cần báo cáo");
        }
        GuardAssignment assignment = requireAssignment(shift, soldier.getId());
        if (assignment.getStatus() == GuardAssignmentStatus.COMPLETED) {
            throw new ConflictException("Kết quả ca gác đã được ghi nhận");
        }
        assignment.setAcknowledgedAt(assignment.getAcknowledgedAt() == null ? Instant.now() : assignment.getAcknowledgedAt());
        assignment.setStatus(GuardAssignmentStatus.COMPLETED);
        assignment.setResultCondition(request.resultCondition());
        assignment.setResultContent(request.resultContent().trim());
        assignment.setIncidentNote(blankToNull(request.incidentNote()));
        assignment.setCompletedAt(Instant.now());
        GuardShift saved = shiftRepository.save(shift);
        notificationService.guardResultSubmitted(saved, assignment);
        activityLogService.record(soldier, ActivityAction.GUARD_RESULT_SUBMITTED, ActivityTargetType.GUARD_SHIFT,
                saved.getId(), "Đã gửi kết quả thực hiện ca gác.");
        if (assignment.getResultCondition() != GuardResultCondition.NORMAL) {
            activityLogService.record(soldier, ActivityAction.GUARD_INCIDENT_REPORTED, ActivityTargetType.GUARD_SHIFT,
                    saved.getId(), "Đã ghi nhận sự cố hoặc vấn đề phát sinh trong ca gác.");
        }
        return toResponse(saved, soldier);
    }

    private void validateRequest(GuardShiftRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (request.status() == GuardShiftStatus.CANCELLED) {
            throw new BusinessException("Không thể tạo hoặc cập nhật trực tiếp một ca ở trạng thái đã hủy");
        }
        if (!MAIN_GATE.equalsIgnoreCase(request.location().trim())) {
            throw new BusinessException("Vị trí trực gác duy nhất của đơn vị là Cổng chính");
        }
        Set<Long> distinct = new HashSet<>(request.soldierIds());
        if (distinct.size() != request.soldierIds().size()) {
            throw new BusinessException("Danh sách phân công có chiến sĩ bị trùng");
        }
    }

    private void requireEditable(GuardShift shift) {
        if (shift.getStatus() == GuardShiftStatus.CANCELLED) {
            throw new ConflictException("Không thể sửa ca gác đã hủy");
        }
        if (shift.getStatus() == GuardShiftStatus.PUBLISHED
                && shift.getOccurrenceStatus(businessClock.instant()) != OccurrenceStatus.UPCOMING) {
            throw new ConflictException("Chỉ được sửa ca đã công bố khi ca gác chưa bắt đầu");
        }
    }

    private void apply(GuardShift shift, GuardShiftRequest request) {
        shift.setStartAt(request.startAt());
        shift.setEndAt(request.endAt());
        shift.setLocation(MAIN_GATE);
        shift.setNote(blankToNull(request.note()));
        shift.setStatus(request.status());
    }

    private void replaceAssignments(GuardShift shift, List<Long> soldierIds) {
        List<UserAccount> users = userRepository.findAllById(new LinkedHashSet<>(soldierIds));
        if (users.size() != soldierIds.size()) {
            throw new NotFoundException("Có chiến sĩ được phân công không tồn tại");
        }
        Map<Long, UserAccount> usersById = users.stream().collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        for (Long soldierId : soldierIds) {
            UserAccount soldier = usersById.get(soldierId);
            if (soldier.getRole() != UserRole.SOLDIER) {
                throw new BusinessException("Chỉ tài khoản chiến sĩ mới được phân công");
            }
            if (!soldier.isActive()) {
                throw new BusinessException("Không thể phân công chiến sĩ đã bị vô hiệu hóa: " + soldier.getFullName());
            }
        }

        Set<Long> requested = new LinkedHashSet<>(soldierIds);
        shift.getAssignments().removeIf(item -> !requested.contains(item.getSoldier().getId()));
        Set<Long> existing = shift.getAssignments().stream()
                .map(item -> item.getSoldier().getId()).collect(Collectors.toSet());
        for (Long soldierId : soldierIds) {
            if (!existing.contains(soldierId)) {
                GuardAssignment assignment = new GuardAssignment();
                assignment.setGuardShift(shift);
                assignment.setSoldier(usersById.get(soldierId));
                assignment.setStatus(GuardAssignmentStatus.ASSIGNED);
                shift.getAssignments().add(assignment);
            }
        }
    }

    private GuardAssignment requireAssignment(GuardShift shift, Long soldierId) {
        return shift.getAssignments().stream()
                .filter(item -> item.getSoldier().getId().equals(soldierId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("Chiến sĩ không được phân công vào ca gác này"));
    }

    private List<UserAccount> assignedUsers(GuardShift shift) {
        return shift.getAssignments().stream().map(GuardAssignment::getSoldier).toList();
    }

    private GuardShift getShift(Long shiftId) {
        return shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca gác"));
    }

    private GuardShiftResponse toResponse(GuardShift shift, UserAccount viewer) {
        Instant now = businessClock.instant();
        OccurrenceStatus occurrence = shift.getOccurrenceStatus(now);
        List<AssignmentResponse> assignments = shift.getAssignments().stream()
                .sorted(Comparator.comparing(item -> item.getSoldier().getFullName()))
                .map(assignment -> toAssignmentResponse(assignment,
                        viewer.getRole() == UserRole.COMMANDER || assignment.getSoldier().getId().equals(viewer.getId())))
                .toList();
        boolean allAcknowledged = !assignments.isEmpty()
                && assignments.stream().allMatch(item -> item.acknowledgedAt() != null);
        boolean hasIncident = assignments.stream().anyMatch(item ->
                item.resultCondition() == GuardResultCondition.INCIDENT
                        || item.resultCondition() == GuardResultCondition.REQUIRES_REPORT);
        ZonedDateTime localStart = shift.getStartAt().toInstant().atZone(BusinessTime.ZONE_ID);
        ZonedDateTime localEnd = shift.getEndAt().toInstant().atZone(BusinessTime.ZONE_ID);
        String displayStatus;
        if (shift.getStatus() == GuardShiftStatus.DRAFT) {
            displayStatus = "DRAFT";
        } else if (shift.getStatus() == GuardShiftStatus.CANCELLED) {
            displayStatus = "CANCELLED";
        } else if (occurrence == OccurrenceStatus.COMPLETED) {
            displayStatus = "COMPLETED";
        } else {
            displayStatus = "PUBLISHED";
        }
        return new GuardShiftResponse(
                shift.getId(), shift.getStartAt(), shift.getEndAt(), localStart.toLocalDate(),
                localStart.toLocalTime(), localEnd.toLocalTime(), shift.getLocation(), shift.getNote(),
                shift.getStatus(), occurrence, displayStatus, toBrief(shift.getCreatedBy()),
                assignments.stream().map(AssignmentResponse::soldier).toList(), assignments,
                allAcknowledged, hasIncident, shift.getCreatedAt(), shift.getUpdatedAt()
        );
    }

    private AssignmentResponse toAssignmentResponse(GuardAssignment assignment, boolean includeSensitiveResult) {
        return new AssignmentResponse(
                assignment.getId(), toBrief(assignment.getSoldier()), assignment.getAcknowledgedAt(),
                assignment.getStatus(), includeSensitiveResult ? assignment.getResultCondition() : null,
                includeSensitiveResult ? assignment.getResultContent() : null,
                includeSensitiveResult ? assignment.getIncidentNote() : null,
                includeSensitiveResult ? assignment.getCompletedAt() : null
        );
    }

    private SoldierBrief toBrief(UserAccount user) {
        return new SoldierBrief(user.getId(), user.getUsername(), user.getFullName(), user.getRankOrPosition(), user.isActive());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
