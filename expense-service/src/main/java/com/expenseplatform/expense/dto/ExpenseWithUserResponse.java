package com.expenseplatform.expense.dto;

import com.expenseplatform.expense.entity.ExpenseCategory;
import com.expenseplatform.expense.entity.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseWithUserResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private BigDecimal amount;
    private ExpenseCategory category;
    private String description;
    private ExpenseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
