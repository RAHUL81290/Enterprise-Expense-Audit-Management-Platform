package com.expenseplatform.expense.controller;

import com.expenseplatform.expense.dto.*;
import com.expenseplatform.expense.security.AuthenticatedUser;
import com.expenseplatform.expense.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody CreateExpenseRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return new ResponseEntity<>(expenseService.createExpense(request, user), HttpStatus.CREATED);
    }

    // NOTE: /summary and /{id}/with-user must be declared BEFORE /{id}
    // or Spring will try to parse "summary"/"with-user" as a Long path variable.

    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummaryResponse> getSummary() {
        return ResponseEntity.ok(expenseService.getSummary());
    }

    @GetMapping("/{id}/with-user")
    public ResponseEntity<ExpenseWithUserResponse> getWithUserDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(expenseService.getExpenseWithUserDetails(id, user, authHeader));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getById(@PathVariable Long id,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(expenseService.getExpenseById(id, user));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ExpenseResponse>> getMy(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(expenseService.getMyExpenses(user, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<ExpenseResponse>> getAll(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(expenseService.getAllExpenses(user, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateExpenseRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        expenseService.deleteExpense(id, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ExpenseResponse> approve(@PathVariable Long id,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(expenseService.approveExpense(id, user));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ExpenseResponse> reject(@PathVariable Long id,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(expenseService.rejectExpense(id, user));
    }
}
