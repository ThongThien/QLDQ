package com.qldq.qldqapi.repository;

import com.qldq.qldqapi.entity.Notification;
import com.qldq.qldqapi.entity.enums.NotificationReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAllByRecipientId(Long recipientId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    boolean existsByEventKey(String eventKey);

    boolean existsByRecipientIdAndReferenceTypeAndReferenceId(
            Long recipientId, NotificationReferenceType referenceType, Long referenceId);

    @Modifying
    @Query("""
            update Notification n set n.readAt = :readAt
            where n.recipient.id = :recipientId and n.readAt is null
            """)
    int markAllRead(@Param("recipientId") Long recipientId, @Param("readAt") Instant readAt);
}
