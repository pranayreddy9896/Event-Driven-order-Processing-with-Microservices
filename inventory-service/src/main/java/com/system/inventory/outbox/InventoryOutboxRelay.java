package com.system.inventory.outbox;

import com.system.common.outbox.OutboxEvent;
import com.system.common.outbox.OutboxEventRepository;
import com.system.common.outbox.OutboxStatus;
import com.system.common.tracing.TraceContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryOutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TraceContextHelper traceContextHelper;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        event.getTopic(),
                        event.getAggregateId(),
                        event.getPayload()
                );
                traceContextHelper.injectTraceHeaders(record, event.getTraceId());

                kafkaTemplate.send(record).get();

                event.setStatus(OutboxStatus.SENT);
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);

                log.info("Inventory Outbox Relay published [id={}, topic={}, aggId={}]",
                        event.getId(), event.getTopic(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish inventory outbox event {}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                if (event.getRetryCount() >= 5) {
                    event.setStatus(OutboxStatus.FAILED);
                }
                outboxEventRepository.save(event);
            }
        }
    }
}
