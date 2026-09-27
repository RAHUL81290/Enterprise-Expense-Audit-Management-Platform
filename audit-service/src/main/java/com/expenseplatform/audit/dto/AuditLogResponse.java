package com.expenseplatform.audit.dto;

import com.expenseplatform.audit.entity.AuditEventType;
import com.expenseplatform.audit.entity.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private AuditEventType event;
    private String service;
    private AuditStatus status;
    private String description;
    private String correlationId;
    private LocalDateTime timestamp;
}
