package com.system.dlq.listener;

import com.system.common.model.Topics;
import com.system.dlq.service.DlqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqKafkaListener {

    private final DlqService dlqService;

    @KafkaListener(
            topics = {
                    Topics.ORDER_CREATED_DLT,
                    Topics.INVENTORY_RESERVED_DLT,
                    Topics.PAYMENT_COMPLETED_DLT,
                    "order.created-dlt",
                    "inventory.reserved-dlt",
                    "payment.completed-dlt"
            },
            groupId = "dlq-admin-group"
    )
    public void captureDeadLetter(ConsumerRecord<String, String> record) {
        log.warn("DLQ ADMIN CAPTURED DEAD LETTER: topic=[{}], key=[{}], offset=[{}]",
                record.topic(), record.key(), record.offset());

        String originalTopic = record.topic().replaceAll("(?i)\\.dlt|-dlt", "");
        String dltTopic = record.topic();
        String key = record.key();
        String payload = record.value();

        StringJoiner headersJoiner = new StringJoiner(", ");
        String exceptionMsg = "Unknown failure / Poison pill";

        for (Header header : record.headers()) {
            String headerKey = header.key();
            String headerVal = header.value() != null ? new String(header.value(), StandardCharsets.UTF_8) : "null";
            headersJoiner.add(headerKey + "=" + headerVal);
            if (headerKey.toLowerCase().contains("exception-message")) {
                exceptionMsg = headerVal;
            }
        }

        dlqService.recordDeadLetter(
                originalTopic,
                dltTopic,
                key,
                payload,
                exceptionMsg,
                headersJoiner.toString()
        );
    }
}
