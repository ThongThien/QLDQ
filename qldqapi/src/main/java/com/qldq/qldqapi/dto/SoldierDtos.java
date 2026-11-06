package com.qldq.qldqapi.dto;

import com.qldq.qldqapi.entity.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class SoldierDtos {
    private SoldierDtos() {
    }

    public record SoldierCreateRequest(
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username chỉ được chứa chữ, số, dấu chấm, gạch dưới hoặc gạch ngang")
            String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 100) String rankOrPosition,
            Boolean active
    ) {
    }

    public record SoldierResponse(
            Long id,
            String username,
            String fullName,
            UserRole role,
            String rankOrPosition,
            boolean active,
            long assignedShiftCount,
            long completedShiftCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
