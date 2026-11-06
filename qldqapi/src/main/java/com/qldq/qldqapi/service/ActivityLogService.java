package com.qldq.qldqapi.service;

import com.qldq.qldqapi.dto.ActivityLogDtos.ActivityActor;
import com.qldq.qldqapi.dto.ActivityLogDtos.ActivityLogResponse;
import com.qldq.qldqapi.dto.PagedResponse;
import com.qldq.qldqapi.entity.ActivityLog;
import com.qldq.qldqapi.entity.UserAccount;
import com.qldq.qldqapi.entity.enums.ActivityAction;
import com.qldq.qldqapi.entity.enums.ActivityTargetType;
import com.qldq.qldqapi.repository.ActivityLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ActivityLogService {
    private final ActivityLogRepository repository;
    private final UserAccessService accessService;

    public ActivityLogService(ActivityLogRepository repository, UserAccessService accessService) {
        this.repository = repository;
        this.accessService = accessService;
    }

    public void record(UserAccount actor, ActivityAction action, ActivityTargetType targetType,
                       Long targetId, String description) {
        ActivityLog log = new ActivityLog();
        log.setActor(actor);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ActivityLogResponse> search(
            Long actorId, Instant from, Instant to, ActivityAction action,
            ActivityTargetType targetType, Long targetId, Pageable pageable
    ) {
        accessService.requireCommander(actorId);
        Specification<ActivityLog> filters = (root, query, builder) -> builder.conjunction();
        if (from != null) {
            filters = filters.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            filters = filters.and((root, query, builder) -> builder.lessThan(root.get("createdAt"), to));
        }
        if (action != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("action"), action));
        }
        if (targetType != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("targetType"), targetType));
        }
        if (targetId != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("targetId"), targetId));
        }
        Page<ActivityLog> page = repository.findAll(filters, pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        UserAccount actor = log.getActor();
        return new ActivityLogResponse(log.getId(),
                new ActivityActor(actor.getId(), actor.getUsername(), actor.getFullName(), actor.getRole().name()),
                log.getAction(), log.getTargetType(), log.getTargetId(), log.getDescription(), log.getCreatedAt());
    }
}
