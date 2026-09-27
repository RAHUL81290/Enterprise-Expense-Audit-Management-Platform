package com.expenseplatform.audit.event;

import com.expenseplatform.audit.dto.AuditEventMessage;
import com.expenseplatform.audit.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditEventConsumer {

    private static final String MDC_CORRELATION_KEY = "correlationId";

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditService auditService) {
        this.auditService = auditService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "audit-events", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            AuditEventMessage event = objectMapper.readValue(message, AuditEventMessage.class);

            MDC.put(MDC_CORRELATION_KEY, event.getCorrelationId());

            log.info("Received audit event from Kafka: event={}, userId={}", event.getEvent(), event.getUserId());
            auditService.saveAuditLog(event);

        } catch (Exception e) {
            // Poison-pill safety: never let a malformed message crash this thread
            // or trigger infinite redelivery. Log and move on.
            log.error("Failed to process audit event. Raw message: {}. Error: {}", message, e.getMessage());
        } finally {
            MDC.remove(MDC_CORRELATION_KEY);
        }
    }
}
