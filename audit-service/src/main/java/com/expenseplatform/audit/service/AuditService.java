package com.expenseplatform.audit.service;

import com.expenseplatform.audit.dto.AuditEventMessage;
import com.expenseplatform.audit.dto.AuditLogFilterRequest;
import com.expenseplatform.audit.dto.AuditLogResponse;
import com.expenseplatform.audit.entity.AuditEventType;
import com.expenseplatform.audit.entity.AuditLog;
import com.expenseplatform.audit.entity.AuditStatus;
import com.expenseplatform.audit.exception.ResourceNotFoundException;
import com.expenseplatform.audit.repository.AuditLogRepository;
import com.expenseplatform.audit.repository.AuditLogSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void saveAuditLog(AuditEventMessage event) {
        AuditLog auditLog = AuditLog.builder()
                .userId(event.getUserId())
                .event(AuditEventType.valueOf(event.getEvent()))
                .service(event.getService())
                .status(AuditStatus.valueOf(event.getStatus()))
                .description(event.getDescription())
                .correlationId(event.getCorrelationId())
                .timestamp(event.getTimestamp())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log persisted: id={}, event={}, userId={}", saved.getId(), saved.getEvent(), saved.getUserId());
    }

    public AuditLogResponse getAuditLogById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found with id: " + id));
        return toResponse(auditLog);
    }

    public Page<AuditLogResponse> getFilteredAuditLogs(AuditLogFilterRequest filter, Pageable pageable) {
        var specification = AuditLogSpecification.withFilters(filter);
        return auditLogRepository.findAll(specification, pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .event(auditLog.getEvent())
                .service(auditLog.getService())
                .status(auditLog.getStatus())
                .description(auditLog.getDescription())
                .correlationId(auditLog.getCorrelationId())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}
