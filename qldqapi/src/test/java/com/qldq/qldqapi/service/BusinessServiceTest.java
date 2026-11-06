package com.qldq.qldqapi.service;

import com.qldq.qldqapi.dto.GuardDtos.GuardShiftRequest;
import com.qldq.qldqapi.config.BusinessClock;
import com.qldq.qldqapi.dto.GuardDtos.GuardShiftResponse;
import com.qldq.qldqapi.dto.WorkScheduleDtos.ScheduleRequest;
import com.qldq.qldqapi.entity.GuardShift;
import com.qldq.qldqapi.entity.UserAccount;
import com.qldq.qldqapi.entity.enums.*;
import com.qldq.qldqapi.exception.BusinessException;
import com.qldq.qldqapi.repository.GuardShiftRepository;
import com.qldq.qldqapi.repository.NotificationRepository;
import com.qldq.qldqapi.repository.UserAccountRepository;
import com.qldq.qldqapi.repository.WorkItemRepository;
import com.qldq.qldqapi.repository.WorkScheduleRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BusinessServiceTest {

    @Test
    void acceptsOvernightGuardShift() {
        GuardShiftRepository shiftRepository = mock(GuardShiftRepository.class);
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        UserAccessService accessService = mock(UserAccessService.class);
        UserAccount commander = user(1L, "chihuy", "Chỉ huy", UserRole.COMMANDER, true);
        UserAccount soldier = user(2L, "chiensi01", "Chiến sĩ Một", UserRole.SOLDIER, true);
        when(accessService.requireCommander(1L)).thenReturn(commander);
        when(userRepository.findAllById(any())).thenReturn(List.of(soldier));
        when(shiftRepository.save(any(GuardShift.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GuardShiftService service = new GuardShiftService(
                shiftRepository, userRepository, accessService,
                mock(NotificationService.class), mock(ActivityLogService.class), new BusinessClock());

        GuardShiftResponse result = service.create(1L, new GuardShiftRequest(
                OffsetDateTime.parse("2026-11-05T22:00:00+07:00"),
                OffsetDateTime.parse("2026-11-06T00:00:00+07:00"),
                "Cổng chính", null, GuardShiftStatus.DRAFT, List.of(2L)
        ));

        assertEquals(LocalDate.of(2026, 11, 5), result.date());
        assertEquals(1, result.assignments().size());
    }

    @Test
    void rejectsInactiveSoldierAssignment() {
        GuardShiftRepository shiftRepository = mock(GuardShiftRepository.class);
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        UserAccessService accessService = mock(UserAccessService.class);
        when(accessService.requireCommander(1L))
                .thenReturn(user(1L, "chihuy", "Chỉ huy", UserRole.COMMANDER, true));
        when(userRepository.findAllById(any()))
                .thenReturn(List.of(user(2L, "chiensi01", "Chiến sĩ Một", UserRole.SOLDIER, false)));
        GuardShiftService service = new GuardShiftService(
                shiftRepository, userRepository, accessService,
                mock(NotificationService.class), mock(ActivityLogService.class), new BusinessClock());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(1L,
                new GuardShiftRequest(
                        OffsetDateTime.parse("2026-11-05T07:00:00+07:00"),
                        OffsetDateTime.parse("2026-11-05T09:00:00+07:00"),
                        "Cổng chính", null, GuardShiftStatus.DRAFT, List.of(2L)
                )));

        assertTrue(exception.getMessage().contains("vô hiệu hóa"));
        verifyNoInteractions(shiftRepository);
    }

    @Test
    void rejectsScheduleThatDoesNotStartOnMonday() {
        WorkScheduleService service = new WorkScheduleService(
                mock(WorkScheduleRepository.class), mock(WorkItemRepository.class),
                mock(UserAccountRepository.class), mock(UserAccessService.class),
                mock(NotificationService.class), mock(NotificationRepository.class), mock(ActivityLogService.class),
                new BusinessClock()
        );

        assertThrows(BusinessException.class, () -> service.create(1L, ScheduleType.WEEKLY_WORK,
                new ScheduleRequest("Lịch sai tuần", LocalDate.of(2026, 11, 3),
                        null, null, ScheduleStatus.DRAFT)));
    }

    private UserAccount user(Long id, String username, String fullName, UserRole role, boolean active) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
