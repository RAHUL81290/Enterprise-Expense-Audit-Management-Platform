package com.expenseplatform.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSummaryResponse implements Serializable {
    private long totalExpenses;
    private long pendingExpenses;
    private long approvedExpenses;
    private long rejectedExpenses;
    private BigDecimal totalAmount;
}
