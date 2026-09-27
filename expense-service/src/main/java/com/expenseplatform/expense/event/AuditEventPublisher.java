package com.expenseplatform.expense.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class AuditEventPublisher {

    private static final String TOPIC = "audit-events";
    private static final String SERVICE_NAME = "expense-service";
    private static final String MDC_CORRELATION_KEY = "correlationId";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AuditEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void publish(Long userId, String eventType, String description) {
        String correlationId = MDC.get(MDC_CORRELATION_KEY);

        AuditEventMessage message = AuditEventMessage.builder()
                .userId(userId)
                .event(eventType)
                .service(SERVICE_NAME)
                .status("SUCCESS")
                .description(description)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(message);

            kafkaTemplate.send(TOPIC, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish audit event: {} for userId={}. Error: {}",
                            eventType, userId, ex.getMessage());
                } else {
                    log.info("Audit event published: {} for userId={}", eventType, userId);
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize audit event: {} for userId={}. Error: {}",
                    eventType, userId, e.getMessage());
        }
    }
}
