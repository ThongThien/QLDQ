package com.qldq.qldqapi.service;

import com.qldq.qldqapi.entity.UserAccount;
import com.qldq.qldqapi.entity.enums.UserRole;
import com.qldq.qldqapi.exception.ForbiddenException;
import com.qldq.qldqapi.exception.NotFoundException;
import com.qldq.qldqapi.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class UserAccessService {
    private final UserAccountRepository userRepository;

    public UserAccessService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserAccount requireActiveUser(Long userId) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản người dùng"));
        if (!user.isActive()) {
            throw new ForbiddenException("Tài khoản đã bị vô hiệu hóa");
        }
        return user;
    }

    public UserAccount requireCommander(Long userId) {
        UserAccount user = requireActiveUser(userId);
        if (user.getRole() != UserRole.COMMANDER) {
            throw new ForbiddenException("Chỉ huy mới được thực hiện thao tác này");
        }
        return user;
    }

    public UserAccount requireSoldier(Long userId) {
        UserAccount user = requireActiveUser(userId);
        if (user.getRole() != UserRole.SOLDIER) {
            throw new ForbiddenException("Thao tác này chỉ dành cho chiến sĩ");
        }
        return user;
    }
}
