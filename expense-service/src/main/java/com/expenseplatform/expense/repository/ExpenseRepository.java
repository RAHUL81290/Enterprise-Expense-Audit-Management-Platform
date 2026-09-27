package com.expenseplatform.expense.repository;

import com.expenseplatform.expense.entity.Expense;
import com.expenseplatform.expense.entity.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<Expense> findAll(Pageable pageable);

    long countByStatus(ExpenseStatus status);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
    BigDecimal sumAllAmounts();
}
