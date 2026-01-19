package com.pm.notificationservice.repository;

import com.pm.notificationservice.model.Notification;
import com.pm.notificationservice.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {


    Optional<Notification> findByUserId(UUID userId);

    Optional<Notification> findByStatus(NotificationStatus status);

    Optional<Notification> findByScheduledAtBefore(LocalDateTime time);



}
