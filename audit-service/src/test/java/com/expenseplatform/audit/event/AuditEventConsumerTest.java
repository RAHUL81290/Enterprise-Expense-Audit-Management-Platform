package com.expenseplatform.audit.event;

import com.expenseplatform.audit.dto.AuditEventMessage;
import com.expenseplatform.audit.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock
    private AuditService auditService;

    private AuditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuditEventConsumer(auditService);
    }

    @Test
    void consume_shouldDeserializeAndDelegateToAuditService_whenMessageIsValid() {
        String validJson = """
                {
                  "userId": 1,
                  "event": "EXPENSE_CREATED",
                  "service": "expense-service",
                  "status": "SUCCESS",
                  "description": "Expense created",
                  "correlationId": "abc-123",
                  "timestamp": "2026-09-22T12:00:00"
                }
                """;

        consumer.consume(validJson);

        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditService).saveAuditLog(captor.capture());

        AuditEventMessage captured = captor.getValue();
        assertThat(captured.getUserId()).isEqualTo(1L);
        assertThat(captured.getEvent()).isEqualTo("EXPENSE_CREATED");
        assertThat(captured.getCorrelationId()).isEqualTo("abc-123");
    }

    @Test
    void consume_shouldNotThrow_andShouldNotCallAuditService_whenMessageIsMalformedJson() {
        String malformedJson = "{ this is not valid json at all";

        consumer.consume(malformedJson);

        verify(auditService, never()).saveAuditLog(any());
    }

    @Test
    void consume_shouldNotThrow_whenEventFieldIsAnInvalidEnumValue() {
        String jsonWithBadEnum = """
                {
                  "userId": 1,
                  "event": "TOTALLY_MADE_UP_EVENT",
                  "service": "expense-service",
                  "status": "SUCCESS",
                  "description": "test",
                  "timestamp": "2026-09-22T12:00:00"
                }
                """;

        doThrow(new IllegalArgumentException("bad enum")).when(auditService).saveAuditLog(any());

        consumer.consume(jsonWithBadEnum);
        // No assertion needed beyond "this didn't throw" - JUnit fails the test
        // automatically if consume() lets an exception propagate out.
    }
}
