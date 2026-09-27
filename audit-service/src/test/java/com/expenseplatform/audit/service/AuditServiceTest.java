package com.expenseplatform.audit.service;

import com.expenseplatform.audit.dto.AuditEventMessage;
import com.expenseplatform.audit.entity.AuditEventType;
import com.expenseplatform.audit.entity.AuditLog;
import com.expenseplatform.audit.entity.AuditStatus;
import com.expenseplatform.audit.exception.ResourceNotFoundException;
import com.expenseplatform.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private AuditEventMessage eventMessage;

    @BeforeEach
    void setUp() {
        eventMessage = AuditEventMessage.builder()
                .userId(1L)
                .event("EXPENSE_CREATED")
                .service("expense-service")
                .status("SUCCESS")
                .description("Expense created")
                .correlationId("test-correlation-id")
                .timestamp(LocalDateTime.of(2026, 9, 22, 12, 0, 0))
                .build();
    }

    @Test
    void saveAuditLog_shouldMapEventMessageFieldsCorrectlyOntoEntity() {
        when(auditLogRepository.save(auditLogCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        auditService.saveAuditLog(eventMessage);

        AuditLog captured = auditLogCaptor.getValue();
        assertThat(captured.getUserId()).isEqualTo(1L);
        assertThat(captured.getEvent()).isEqualTo(AuditEventType.EXPENSE_CREATED);
        assertThat(captured.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(captured.getCorrelationId()).isEqualTo("test-correlation-id");
        assertThat(captured.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 9, 22, 12, 0, 0));
    }

    @Test
    void saveAuditLog_shouldThrow_whenEventStringDoesNotMatchAnyEnumValue() {
        AuditEventMessage invalidEvent = eventMessage.toBuilder().event("NOT_A_REAL_EVENT").build();

        assertThatThrownBy(() -> auditService.saveAuditLog(invalidEvent))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAuditLogById_shouldThrow_whenNotFound() {
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getAuditLogById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
