package com.expenseplatform.audit.dto;

import java.time.LocalDateTime;

public record AuditLogFilterRequest(
        Long userId,
        String event,
        String status,
        String service,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
}
