package com.system.dlq.service;

import com.system.dlq.model.DeadLetterMessage;
import com.system.dlq.repository.DeadLetterMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqService {

    private final DeadLetterMessageRepository dlqRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public DeadLetterMessage recordDeadLetter(String originalTopic, String dltTopic, String key, String payload, String exception, String headers) {
        DeadLetterMessage msg = DeadLetterMessage.builder()
                .originalTopic(originalTopic)
                .dltTopic(dltTopic)
                .messageKey(key)
                .payload(payload)
                .exceptionMessage(exception)
                .headers(headers)
                .status("POISONED")
                .receivedAt(Instant.now())
                .build();
        return dlqRepository.save(msg);
    }

    /**
     * Replays a dead letter message by resubmitting it to the original topic.
     */
    @Transactional
    public DeadLetterMessage replayMessage(String id) {
        DeadLetterMessage msg = dlqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DLQ Message not found: " + id));

        log.info("REPLAYING DLQ MESSAGE [id={}, targetTopic={}, key={}]: {}",
                id, msg.getOriginalTopic(), msg.getMessageKey(), msg.getPayload());

        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    msg.getOriginalTopic(),
                    msg.getMessageKey(),
                    msg.getPayload()
            );
            kafkaTemplate.send(record).get();

            msg.setStatus("REPLAYED");
            msg.setReplayedAt(Instant.now());
            return dlqRepository.save(msg);
        } catch (Exception e) {
            log.error("Failed to replay message {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Replay failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public int replayAll() {
        List<DeadLetterMessage> poisoned = dlqRepository.findByStatusOrderByReceivedAtDesc("POISONED");
        int count = 0;
        for (DeadLetterMessage msg : poisoned) {
            try {
                replayMessage(msg.getId());
                count++;
            } catch (Exception e) {
                log.error("Error replaying msg {}: {}", msg.getId(), e.getMessage());
            }
        }
        return count;
    }

    @Transactional
    public void discardMessage(String id) {
        dlqRepository.findById(id).ifPresent(msg -> {
            msg.setStatus("DISCARDED");
            dlqRepository.save(msg);
        });
    }

    public List<DeadLetterMessage> getAllMessages() {
        return dlqRepository.findAllByOrderByReceivedAtDesc();
    }

    public List<DeadLetterMessage> getPoisonedMessages() {
        return dlqRepository.findByStatusOrderByReceivedAtDesc("POISONED");
    }
}
