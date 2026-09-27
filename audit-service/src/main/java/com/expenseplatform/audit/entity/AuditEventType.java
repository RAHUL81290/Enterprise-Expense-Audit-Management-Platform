package com.expenseplatform.audit.entity;

public enum AuditEventType {
    LOGIN,
    LOGOUT,
    EXPENSE_CREATED,
    EXPENSE_UPDATED,
    EXPENSE_APPROVED,
    EXPENSE_REJECTED
}
