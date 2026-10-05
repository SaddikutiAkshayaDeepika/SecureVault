package com.securevault.service;

import com.securevault.entity.Notification;
import com.securevault.repository.NotificationRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {

        this.notificationRepository = notificationRepository;
    }

    // =========================
    // CREATE NOTIFICATION
    // =========================

    @CacheEvict(
            value = "unreadNotificationCount",
            key = "#email"
    )
    public Notification createNotification(
            String email,
            String type,
            String title,
            String message,
            String severity) {

        Notification notification = new Notification();

        notification.setEmail(email);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSeverity(severity);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    // =========================
    // GET ALL NOTIFICATIONS
    // =========================

    public List<Notification> getNotifications(String email) {

        return notificationRepository
                .findByEmailOrderByCreatedAtDesc(email);
    }

    // =========================
    // GET UNREAD NOTIFICATIONS
    // =========================

    public List<Notification> getUnreadNotifications(
            String email) {

        return notificationRepository
                .findByEmailAndReadFalseOrderByCreatedAtDesc(
                        email);
    }

    // =========================
    // COUNT UNREAD
    // =========================

    @Cacheable(
            value = "unreadNotificationCount",
            key = "#email"
    )
    public long getUnreadCount(String email) {

        return notificationRepository
                .findByEmailAndReadFalseOrderByCreatedAtDesc(
                        email)
                .size();
    }

    // =========================
    // MARK AS READ
    // =========================

    @CacheEvict(
            value = "unreadNotificationCount",
            key = "#email"
    )
    public boolean markAsRead(
            Long notificationId,
            String email) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElse(null);

        if (notification == null) {
            return false;
        }

        // Make sure the notification belongs
        // to the currently logged-in user.
        if (!notification.getEmail().equals(email)) {
            return false;
        }

        notification.setRead(true);

        notificationRepository.save(notification);

        return true;
    }

    // =========================
    // MARK ALL AS READ
    // =========================

    @CacheEvict(
            value = "unreadNotificationCount",
            key = "#email"
    )
    public void markAllAsRead(String email) {

        List<Notification> notifications =
                notificationRepository
                        .findByEmailAndReadFalseOrderByCreatedAtDesc(
                                email);

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);
    }
}