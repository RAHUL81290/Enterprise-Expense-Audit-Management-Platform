package com.expenseplatform.expense.service;

import com.expenseplatform.expense.client.AuthServiceClient;
import com.expenseplatform.expense.dto.CreateExpenseRequest;
import com.expenseplatform.expense.dto.ExpenseResponse;
import com.expenseplatform.expense.dto.ExpenseSummaryResponse;
import com.expenseplatform.expense.entity.Expense;
import com.expenseplatform.expense.entity.ExpenseCategory;
import com.expenseplatform.expense.entity.ExpenseStatus;
import com.expenseplatform.expense.event.AuditEventPublisher;
import com.expenseplatform.expense.exception.InvalidExpenseException;
import com.expenseplatform.expense.exception.ResourceNotFoundException;
import com.expenseplatform.expense.exception.UnauthorizedException;
import com.expenseplatform.expense.repository.ExpenseRepository;
import com.expenseplatform.expense.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Mock
    private RedisTemplate<String, ExpenseSummaryResponse> redisTemplate;

    @Mock
    private ValueOperations<String, ExpenseSummaryResponse> valueOperations;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private ExpenseService expenseService;

    private AuthenticatedUser regularUser;
    private AuthenticatedUser managerUser;
    private Expense pendingExpense;

    @BeforeEach
    void setUp() {
        regularUser = new AuthenticatedUser(1L, "rahul@example.com", "USER");
        managerUser = new AuthenticatedUser(2L, "priya@example.com", "MANAGER");

        pendingExpense = Expense.builder()
                .id(10L)
                .employeeId(1L)
                .amount(new BigDecimal("500.00"))
                .category(ExpenseCategory.FOOD)
                .description("Team lunch")
                .status(ExpenseStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createExpense_shouldSaveAndPublishAuditEvent() {
        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setCategory(ExpenseCategory.FOOD);
        request.setDescription("Team lunch");

        when(expenseRepository.save(any(Expense.class))).thenReturn(pendingExpense);

        ExpenseResponse response = expenseService.createExpense(request, regularUser);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(ExpenseStatus.PENDING);

        verify(auditEventPublisher).publish(eq(1L), eq("EXPENSE_CREATED"), anyString());
        verify(redisTemplate).delete("expense:summary");
    }

    @Test
    void getExpenseById_shouldThrow_whenExpenseDoesNotExist() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(99L, regularUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getExpenseById_shouldThrow_whenUserIsNotOwnerAndNotManager() {
        AuthenticatedUser someoneElse = new AuthenticatedUser(999L, "other@example.com", "USER");
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(pendingExpense));

        assertThatThrownBy(() -> expenseService.getExpenseById(10L, someoneElse))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getExpenseById_shouldSucceed_whenManagerAccessesSomeoneElsesExpense() {
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(pendingExpense));

        ExpenseResponse response = expenseService.getExpenseById(10L, managerUser);

        assertThat(response.getId()).isEqualTo(10L);
    }

    @Test
    void approveExpense_shouldChangeStatusAndPublishEvent_whenCalledByManager() {
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(pendingExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseResponse response = expenseService.approveExpense(10L, managerUser);

        assertThat(response.getStatus()).isEqualTo(ExpenseStatus.APPROVED);
        verify(auditEventPublisher).publish(eq(2L), eq("EXPENSE_APPROVED"), anyString());
    }

    @Test
    void approveExpense_shouldThrow_whenCalledByRegularUser() {
        assertThatThrownBy(() -> expenseService.approveExpense(10L, regularUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("managers or admins");

        verify(expenseRepository, never()).findById(anyLong());
    }

    @Test
    void approveExpense_shouldThrow_whenExpenseIsNotPending() {
        Expense alreadyApproved = pendingExpense.toBuilder().status(ExpenseStatus.APPROVED).build();
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(alreadyApproved));

        assertThatThrownBy(() -> expenseService.approveExpense(10L, managerUser))
                .isInstanceOf(InvalidExpenseException.class)
                .hasMessageContaining("PENDING");

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void rejectExpense_shouldChangeStatusToRejected() {
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(pendingExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseResponse response = expenseService.rejectExpense(10L, managerUser);

        assertThat(response.getStatus()).isEqualTo(ExpenseStatus.REJECTED);
    }

    @Test
    void getSummary_shouldReturnCachedValue_whenPresentInRedis() {
        ExpenseSummaryResponse cached = ExpenseSummaryResponse.builder()
                .totalExpenses(5).pendingExpenses(2).approvedExpenses(2).rejectedExpenses(1)
                .totalAmount(new BigDecimal("1000.00"))
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("expense:summary")).thenReturn(cached);

        ExpenseSummaryResponse result = expenseService.getSummary();

        assertThat(result.getTotalExpenses()).isEqualTo(5);
        verify(expenseRepository, never()).count();
    }

    @Test
    void getSummary_shouldQueryDatabaseAndCache_whenCacheMisses() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("expense:summary")).thenReturn(null);
        when(expenseRepository.count()).thenReturn(3L);
        when(expenseRepository.countByStatus(ExpenseStatus.PENDING)).thenReturn(2L);
        when(expenseRepository.countByStatus(ExpenseStatus.APPROVED)).thenReturn(1L);
        when(expenseRepository.countByStatus(ExpenseStatus.REJECTED)).thenReturn(0L);
        when(expenseRepository.sumAllAmounts()).thenReturn(new BigDecimal("750.00"));

        ExpenseSummaryResponse result = expenseService.getSummary();

        assertThat(result.getTotalExpenses()).isEqualTo(3);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("750.00");

        verify(valueOperations).set(eq("expense:summary"), any(ExpenseSummaryResponse.class), eq(10L), any());
    }
}
