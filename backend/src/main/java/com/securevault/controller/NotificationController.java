package com.securevault.controller;

import com.securevault.entity.Notification;
import com.securevault.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    // =========================
    // GET ALL NOTIFICATIONS
    // =========================

    @GetMapping
    public ResponseEntity<?> getNotifications() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        List<Notification> notifications =
                notificationService
                        .getNotifications(email);

        return ResponseEntity.ok(notifications);
    }

    // =========================
    // GET UNREAD NOTIFICATIONS
    // =========================

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        List<Notification> notifications =
                notificationService
                        .getUnreadNotifications(email);

        return ResponseEntity.ok(notifications);
    }

    // =========================
    // GET UNREAD COUNT
    // =========================

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        long count =
                notificationService
                        .getUnreadCount(email);

        return ResponseEntity.ok(
                Map.of("count", count)
        );
    }

    // =========================
    // MARK ONE AS READ
    // =========================

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id) {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        boolean success =
                notificationService
                        .markAsRead(id, email);

        if (!success) {
            return ResponseEntity
                    .status(404)
                    .body(
                            Map.of(
                                    "message",
                                    "Notification not found"
                            )
                    );
        }

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Notification marked as read"
                )
        );
    }

    // =========================
    // MARK ALL AS READ
    // =========================

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        notificationService
                .markAllAsRead(email);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "All notifications marked as read"
                )
        );
    }
}