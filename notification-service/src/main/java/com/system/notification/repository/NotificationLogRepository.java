package com.system.notification.repository;

import com.system.notification.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> {
    List<NotificationLog> findByOrderId(String orderId);
    List<NotificationLog> findAllByOrderBySentAtDesc();
}
