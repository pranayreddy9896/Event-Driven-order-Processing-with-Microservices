package com.system.dlq.repository;

import com.system.dlq.model.DeadLetterMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeadLetterMessageRepository extends JpaRepository<DeadLetterMessage, String> {
    List<DeadLetterMessage> findByStatusOrderByReceivedAtDesc(String status);
    List<DeadLetterMessage> findAllByOrderByReceivedAtDesc();
}
