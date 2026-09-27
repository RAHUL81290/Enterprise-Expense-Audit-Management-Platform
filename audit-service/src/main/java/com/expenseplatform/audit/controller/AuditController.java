package com.expenseplatform.audit.controller;

import com.expenseplatform.audit.dto.AuditLogFilterRequest;
import com.expenseplatform.audit.dto.AuditLogResponse;
import com.expenseplatform.audit.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.getAuditLogById(id));
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String event,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        AuditLogFilterRequest filter = new AuditLogFilterRequest(userId, event, status, service, startDate, endDate);
        return ResponseEntity.ok(auditService.getFilteredAuditLogs(filter, pageable));
    }
}
