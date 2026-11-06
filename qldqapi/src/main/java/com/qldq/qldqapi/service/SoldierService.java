package com.qldq.qldqapi.service;

import com.qldq.qldqapi.dto.PagedResponse;
import com.qldq.qldqapi.dto.SoldierDtos.SoldierResponse;
import com.qldq.qldqapi.entity.UserAccount;
import com.qldq.qldqapi.entity.enums.GuardAssignmentStatus;
import com.qldq.qldqapi.entity.enums.UserRole;
import com.qldq.qldqapi.exception.NotFoundException;
import com.qldq.qldqapi.repository.GuardAssignmentRepository;
import com.qldq.qldqapi.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SoldierService {
    private final UserAccountRepository userRepository;
    private final GuardAssignmentRepository assignmentRepository;
    private final UserAccessService accessService;

    public SoldierService(
            UserAccountRepository userRepository,
            GuardAssignmentRepository assignmentRepository,
            UserAccessService accessService
    ) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.accessService = accessService;
    }

    public PagedResponse<SoldierResponse> search(
            Long actorId, String search, Boolean active, Pageable pageable
    ) {
        accessService.requireCommander(actorId);
        String normalized = search == null || search.isBlank() ? null : search.trim();
        Page<UserAccount> page = userRepository.searchByRole(UserRole.SOLDIER, normalized, active, pageable);
        List<SoldierResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.from(page, content);
    }

    public SoldierResponse get(Long actorId, Long soldierId) {
        accessService.requireCommander(actorId);
        UserAccount soldier = userRepository.findById(soldierId)
                .filter(user -> user.getRole() == UserRole.SOLDIER)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chiến sĩ"));
        return toResponse(soldier);
    }

    private SoldierResponse toResponse(UserAccount soldier) {
        return new SoldierResponse(
                soldier.getId(), soldier.getUsername(), soldier.getFullName(), soldier.getRole(),
                soldier.getRankOrPosition(), soldier.isActive(),
                assignmentRepository.countBySoldierId(soldier.getId()),
                assignmentRepository.countBySoldierIdAndStatus(soldier.getId(), GuardAssignmentStatus.COMPLETED),
                soldier.getCreatedAt(), soldier.getUpdatedAt()
        );
    }
}
