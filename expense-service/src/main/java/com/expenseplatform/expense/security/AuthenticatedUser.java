package com.expenseplatform.expense.security;

public record AuthenticatedUser(Long userId, String email, String role) {
}
