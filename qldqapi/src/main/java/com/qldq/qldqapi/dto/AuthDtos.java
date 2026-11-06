package com.qldq.qldqapi.dto;

import com.qldq.qldqapi.entity.enums.UserRole;
import com.qldq.qldqapi.security.AuthenticatedUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 200) String password
    ) {
    }

    public record AuthUserResponse(
            Long id,
            String username,
            String fullName,
            String rankOrPosition,
            UserRole role
    ) {
        public static AuthUserResponse from(AuthenticatedUser user) {
            return new AuthUserResponse(user.id(), user.username(), user.fullName(), user.rankOrPosition(), user.role());
        }
    }

    public record CsrfTokenResponse(String headerName, String parameterName, String token) {
    }
}
