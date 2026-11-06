package com.qldq.qldqapi.security;

import com.qldq.qldqapi.entity.UserAccount;
import com.qldq.qldqapi.entity.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public record AuthenticatedUser(
        Long id,
        String username,
        String password,
        String fullName,
        String rankOrPosition,
        UserRole role,
        boolean active
) implements UserDetails, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static AuthenticatedUser from(UserAccount user) {
        return new AuthenticatedUser(
                user.getId(), user.getUsername(), user.getPasswordHash(), user.getFullName(),
                user.getRankOrPosition(), user.getRole(), user.isActive()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
