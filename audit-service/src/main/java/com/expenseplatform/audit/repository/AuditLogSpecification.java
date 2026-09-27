package com.expenseplatform.audit.repository;

import com.expenseplatform.audit.dto.AuditLogFilterRequest;
import com.expenseplatform.audit.entity.AuditEventType;
import com.expenseplatform.audit.entity.AuditLog;
import com.expenseplatform.audit.entity.AuditStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> withFilters(AuditLogFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.userId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), filter.userId()));
            }

            if (filter.event() != null && !filter.event().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("event"), AuditEventType.valueOf(filter.event())));
            }

            if (filter.status() != null && !filter.status().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), AuditStatus.valueOf(filter.status())));
            }

            if (filter.service() != null && !filter.service().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("service"), filter.service()));
            }

            if (filter.startDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), filter.startDate()));
            }

            if (filter.endDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), filter.endDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
