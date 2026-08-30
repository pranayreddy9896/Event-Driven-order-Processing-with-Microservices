package com.system.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraceContextHelper {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String SPAN_HEADER = "X-Span-Id";
    public static final String TRACEPARENT_HEADER = "traceparent";

    @Autowired(required = false)
    private Tracer tracer;

    /**
     * Gets current trace ID from Tracer or MDC or generates a new one.
     */
    public String getCurrentTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        String mdcTrace = MDC.get("traceId");
        if (mdcTrace != null && !mdcTrace.isBlank()) {
            return mdcTrace;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Injects trace metadata into Kafka ProducerRecord headers.
     */
    public void injectTraceHeaders(ProducerRecord<?, ?> record, String traceId) {
        if (record != null && traceId != null) {
            record.headers().remove(TRACE_HEADER);
            record.headers().add(TRACE_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Extracts trace ID from Kafka ConsumerRecord headers.
     */
    public String extractTraceId(ConsumerRecord<?, ?> record) {
        if (record != null && record.headers() != null) {
            Header header = record.headers().lastHeader(TRACE_HEADER);
            if (header != null && header.value() != null) {
                return new String(header.value(), StandardCharsets.UTF_8);
            }
            Header traceparent = record.headers().lastHeader(TRACEPARENT_HEADER);
            if (traceparent != null && traceparent.value() != null) {
                String tp = new String(traceparent.value(), StandardCharsets.UTF_8);
                // Format: 00-traceid-spanid-01
                String[] parts = tp.split("-");
                if (parts.length >= 2) {
                    return parts[1];
                }
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Sets trace ID in SLF4J MDC for unified log correlation.
     */
    public void setLoggingContext(String traceId) {
        MDC.put("traceId", traceId != null ? traceId : getCurrentTraceId());
    }

    public void clearLoggingContext() {
        MDC.remove("traceId");
    }
}
