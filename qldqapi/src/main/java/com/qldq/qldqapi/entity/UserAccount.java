package com.qldq.qldqapi.entity;

import com.qldq.qldqapi.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "app_users",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_users_username", columnNames = "username"),
        indexes = {
                @Index(name = "idx_app_users_role", columnList = "role"),
                @Index(name = "idx_app_users_active", columnList = "active")
        }
)
public class UserAccount extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "rank_or_position", length = 100)
    private String rankOrPosition;

    @Column(nullable = false)
    private boolean active = true;
}
