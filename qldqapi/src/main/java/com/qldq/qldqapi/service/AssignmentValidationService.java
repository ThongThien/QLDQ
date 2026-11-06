package com.qldq.qldqapi.service;

import com.qldq.qldqapi.config.BusinessTime;
import com.qldq.qldqapi.dto.AssignmentValidationDtos.*;
import com.qldq.qldqapi.entity.GuardAssignment;
import com.qldq.qldqapi.entity.UserAccount;
import com.qldq.qldqapi.entity.WorkItem;
import com.qldq.qldqapi.entity.WorkItemAssignment;
import com.qldq.qldqapi.entity.enums.UserRole;
import com.qldq.qldqapi.exception.BusinessException;
import com.qldq.qldqapi.repository.GuardAssignmentRepository;
import com.qldq.qldqapi.repository.UserAccountRepository;
import com.qldq.qldqapi.repository.WorkItemAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AssignmentValidationService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final GuardAssignmentRepository guardRepository;
    private final WorkItemAssignmentRepository workRepository;
    private final UserAccountRepository userRepository;
    private final UserAccessService accessService;

    public AssignmentValidationService(
            GuardAssignmentRepository guardRepository,
            WorkItemAssignmentRepository workRepository,
            UserAccountRepository userRepository,
            UserAccessService accessService
    ) {
        this.guardRepository = guardRepository;
        this.workRepository = workRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    public AssignmentValidationResponse validateGuard(Long actorId, GuardConflictRequest request) {
        accessService.requireCommander(actorId);
        validateRange(request.startAt(), request.endAt());
        Map<Long, UserAccount> users = users(request.soldierIds());
        List<ConflictWarning> warnings = new ArrayList<>();
        for (GuardAssignment assignment : guardRepository.findConflictingAssignments(
                users.keySet(), request.startAt(), request.endAt(), request.excludeGuardShiftId())) {
            warnings.add(warning(assignment.getSoldier(), "GUARD_SHIFT", assignment.getGuardShift().getId(),
                    assignment.getGuardShift().getStartAt(), assignment.getGuardShift().getEndAt(), "ca gác"));
        }
        LocalDate from = request.startAt().atZoneSameInstant(BusinessTime.ZONE_ID).toLocalDate();
        LocalDate to = request.endAt().atZoneSameInstant(BusinessTime.ZONE_ID).toLocalDate();
        for (WorkItemAssignment assignment : workRepository.findConflictCandidates(users.keySet(), from, to, null)) {
            Range range = range(assignment.getWorkItem());
            if (overlaps(request.startAt().toInstant(), request.endAt().toInstant(), range.start(), range.end())) {
                warnings.add(warning(assignment.getSoldier(), "WORK_ITEM", assignment.getWorkItem().getId(),
                        range.startAt(), range.endAt(), "lịch công tác"));
            }
        }
        return new AssignmentValidationResponse(distinct(warnings));
    }

    public AssignmentValidationResponse validateWork(Long actorId, WorkConflictRequest request) {
        accessService.requireCommander(actorId);
        Range candidate = range(request.workDate(), request.startTime(), request.endTime(), request.allDay());
        Map<Long, UserAccount> users = users(request.soldierIds());
        List<ConflictWarning> warnings = new ArrayList<>();
        for (GuardAssignment assignment : guardRepository.findConflictingAssignments(users.keySet(),
                candidate.startAt(), candidate.endAt(), null)) {
            warnings.add(warning(assignment.getSoldier(), "GUARD_SHIFT", assignment.getGuardShift().getId(),
                    assignment.getGuardShift().getStartAt(), assignment.getGuardShift().getEndAt(), "ca gác"));
        }
        for (WorkItemAssignment assignment : workRepository.findConflictCandidates(
                users.keySet(), request.workDate(), request.workDate(), request.excludeWorkItemId())) {
            Range existing = range(assignment.getWorkItem());
            if (overlaps(candidate.start(), candidate.end(), existing.start(), existing.end())) {
                warnings.add(warning(assignment.getSoldier(), "WORK_ITEM", assignment.getWorkItem().getId(),
                        existing.startAt(), existing.endAt(), "nhiệm vụ công tác khác"));
            }
        }
        return new AssignmentValidationResponse(distinct(warnings));
    }

    private Map<Long, UserAccount> users(List<Long> soldierIds) {
        Set<Long> ids = new LinkedHashSet<>(soldierIds);
        if (ids.size() != soldierIds.size()) throw new BusinessException("Danh sách phân công có chiến sĩ bị trùng");
        List<UserAccount> users = userRepository.findAllById(ids);
        if (users.size() != ids.size()) {
            throw new BusinessException("Danh sách phân công có tài khoản không tồn tại");
        }
        for (UserAccount user : users) {
            if (user.getRole() != UserRole.SOLDIER || !user.isActive()) {
                throw new BusinessException("Chỉ được kiểm tra phân công cho chiến sĩ đang hoạt động");
            }
        }
        return users.stream().collect(Collectors.toMap(UserAccount::getId, Function.identity()));
    }

    private void validateRange(OffsetDateTime start, OffsetDateTime end) {
        if (!end.isAfter(start)) throw new BusinessException("Thời gian kết thúc phải sau thời gian bắt đầu");
    }

    private Range range(WorkItem item) {
        return range(item.getWorkDate(), item.getStartTime(), item.getEndTime(), item.isAllDay());
    }

    private Range range(LocalDate date, LocalTime startTime, LocalTime endTime, boolean allDay) {
        if (!allDay && (startTime == null || endTime == null || !endTime.isAfter(startTime))) {
            throw new BusinessException("Nhiệm vụ theo giờ phải có khoảng thời gian hợp lệ");
        }
        ZonedDateTime start = allDay ? date.atStartOfDay(BusinessTime.ZONE_ID) : date.atTime(startTime).atZone(BusinessTime.ZONE_ID);
        ZonedDateTime end = allDay ? date.plusDays(1).atStartOfDay(BusinessTime.ZONE_ID) : date.atTime(endTime).atZone(BusinessTime.ZONE_ID);
        return new Range(start.toInstant(), end.toInstant(), start.toOffsetDateTime(), end.toOffsetDateTime());
    }

    private boolean overlaps(Instant firstStart, Instant firstEnd, Instant secondStart, Instant secondEnd) {
        return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
    }

    private ConflictWarning warning(UserAccount soldier, String type, Long referenceId,
                                    OffsetDateTime start, OffsetDateTime end, String label) {
        String message = soldier.getFullName() + " đã được phân công " + label + " từ "
                + FORMATTER.format(start.atZoneSameInstant(BusinessTime.ZONE_ID)) + " đến "
                + FORMATTER.format(end.atZoneSameInstant(BusinessTime.ZONE_ID)) + ".";
        return new ConflictWarning(soldier.getId(), soldier.getFullName(), type, referenceId, start, end, message);
    }

    private List<ConflictWarning> distinct(List<ConflictWarning> warnings) {
        return warnings.stream().collect(Collectors.toMap(
                warning -> warning.soldierId() + ":" + warning.conflictType() + ":" + warning.referenceId(),
                Function.identity(), (first, ignored) -> first, LinkedHashMap::new)).values().stream().toList();
    }

    private record Range(Instant start, Instant end, OffsetDateTime startAt, OffsetDateTime endAt) {
    }
}
