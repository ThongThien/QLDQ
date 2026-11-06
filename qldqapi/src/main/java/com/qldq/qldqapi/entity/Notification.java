package com.qldq.qldqapi.entity;

import com.qldq.qldqapi.entity.enums.NotificationReferenceType;
import com.qldq.qldqapi.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_notifications_event_key", columnNames = "event_key"),
        indexes = {
                @Index(name = "idx_notifications_recipient_created", columnList = "recipient_id,created_at"),
                @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id,read_at")
        }
)
public class Notification extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notifications_recipient"))
    private UserAccount recipient;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 1200)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private NotificationReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "read_at", columnDefinition = "timestamptz")
    private Instant readAt;

    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;
}
