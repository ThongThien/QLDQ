package com.qldq.qldqapi.entity;

import com.qldq.qldqapi.entity.enums.ActivityAction;
import com.qldq.qldqapi.entity.enums.ActivityTargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_activity_logs_action_created", columnList = "action,created_at"),
        @Index(name = "idx_activity_logs_target", columnList = "target_type,target_id")
})
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_activity_logs_actor"))
    private UserAccount actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActivityAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ActivityTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 700)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;
}
