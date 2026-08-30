package com.system.notification.controller;

import com.system.notification.model.NotificationLog;
import com.system.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationLog>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<NotificationLog>> getNotificationsForOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(notificationService.getNotificationsByOrderId(orderId));
    }
}
