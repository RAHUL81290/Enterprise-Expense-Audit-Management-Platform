package com.expenseplatform.expense.service;

import com.expenseplatform.expense.client.AuthServiceClient;
import com.expenseplatform.expense.client.UserDetailsResponse;
import com.expenseplatform.expense.dto.*;
import com.expenseplatform.expense.entity.Expense;
import com.expenseplatform.expense.entity.ExpenseStatus;
import com.expenseplatform.expense.event.AuditEventPublisher;
import com.expenseplatform.expense.exception.InvalidExpenseException;
import com.expenseplatform.expense.exception.ResourceNotFoundException;
import com.expenseplatform.expense.exception.UnauthorizedException;
import com.expenseplatform.expense.repository.ExpenseRepository;
import com.expenseplatform.expense.security.AuthenticatedUser;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ExpenseService {

    private static final String SUMMARY_CACHE_KEY = "expense:summary";
    private static final long CACHE_TTL_MINUTES = 10;

    private final ExpenseRepository expenseRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final RedisTemplate<String, ExpenseSummaryResponse> redisTemplate;
    private final AuthServiceClient authServiceClient;

    public ExpenseService(ExpenseRepository expenseRepository,
                           AuditEventPublisher auditEventPublisher,
                           RedisTemplate<String, ExpenseSummaryResponse> redisTemplate,
                           AuthServiceClient authServiceClient) {
        this.expenseRepository = expenseRepository;
        this.auditEventPublisher = auditEventPublisher;
        this.redisTemplate = redisTemplate;
        this.authServiceClient = authServiceClient;
    }

    public ExpenseResponse createExpense(CreateExpenseRequest request, AuthenticatedUser user) {
        Expense expense = Expense.builder()
                .employeeId(user.userId())
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .status(ExpenseStatus.PENDING)
                .build();

        Expense saved = expenseRepository.save(expense);
        log.info("Expense created: id={}, employeeId={}, amount={}", saved.getId(), saved.getEmployeeId(), saved.getAmount());

        auditEventPublisher.publish(user.userId(), "EXPENSE_CREATED",
                "Expense created with amount " + saved.getAmount() + " in category " + saved.getCategory());
        invalidateSummaryCache();

        return toResponse(saved);
    }

    public ExpenseResponse getExpenseById(Long id, AuthenticatedUser user) {
        Expense expense = findExpenseOrThrow(id);
        enforceOwnershipOrManagerRole(expense, user);
        return toResponse(expense);
    }

    public Page<ExpenseResponse> getMyExpenses(AuthenticatedUser user, Pageable pageable) {
        return expenseRepository.findByEmployeeId(user.userId(), pageable)
                .map(this::toResponse);
    }

    public Page<ExpenseResponse> getAllExpenses(AuthenticatedUser user, Pageable pageable) {
        requireManagerOrAdmin(user);
        return expenseRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public ExpenseResponse updateExpense(Long id, UpdateExpenseRequest request, AuthenticatedUser user) {
        Expense expense = findExpenseOrThrow(id);
        enforceOwnershipOrManagerRole(expense, user);

        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new InvalidExpenseException("Only PENDING expenses can be edited");
        }

        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());

        Expense updated = expenseRepository.save(expense);
        log.info("Expense updated: id={}", updated.getId());

        auditEventPublisher.publish(user.userId(), "EXPENSE_UPDATED", "Expense id " + updated.getId() + " updated");
        invalidateSummaryCache();

        return toResponse(updated);
    }

    public void deleteExpense(Long id, AuthenticatedUser user) {
        Expense expense = findExpenseOrThrow(id);
        enforceOwnershipOrManagerRole(expense, user);

        expenseRepository.delete(expense);
        log.info("Expense deleted: id={}", id);

        invalidateSummaryCache();
    }

    public ExpenseResponse approveExpense(Long id, AuthenticatedUser user) {
        requireManagerOrAdmin(user);
        Expense expense = findExpenseOrThrow(id);

        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new InvalidExpenseException("Only PENDING expenses can be approved");
        }

        expense.setStatus(ExpenseStatus.APPROVED);
        Expense updated = expenseRepository.save(expense);
        log.info("Expense approved: id={}, by userId={}", id, user.userId());

        auditEventPublisher.publish(user.userId(), "EXPENSE_APPROVED",
                "Expense id " + id + " approved for employeeId " + updated.getEmployeeId());
        invalidateSummaryCache();

        return toResponse(updated);
    }

    public ExpenseResponse rejectExpense(Long id, AuthenticatedUser user) {
        requireManagerOrAdmin(user);
        Expense expense = findExpenseOrThrow(id);

        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new InvalidExpenseException("Only PENDING expenses can be rejected");
        }

        expense.setStatus(ExpenseStatus.REJECTED);
        Expense updated = expenseRepository.save(expense);
        log.info("Expense rejected: id={}, by userId={}", id, user.userId());

        auditEventPublisher.publish(user.userId(), "EXPENSE_REJECTED",
                "Expense id " + id + " rejected for employeeId " + updated.getEmployeeId());
        invalidateSummaryCache();

        return toResponse(updated);
    }

    public ExpenseSummaryResponse getSummary() {
        ExpenseSummaryResponse cached = redisTemplate.opsForValue().get(SUMMARY_CACHE_KEY);

        if (cached != null) {
            log.info("Expense summary served from Redis cache");
            return cached;
        }

        log.info("Cache miss for expense summary - querying database");
        ExpenseSummaryResponse summary = buildSummaryFromDatabase();

        redisTemplate.opsForValue().set(SUMMARY_CACHE_KEY, summary, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("Expense summary cached in Redis with TTL of {} minutes", CACHE_TTL_MINUTES);

        return summary;
    }

    public ExpenseWithUserResponse getExpenseWithUserDetails(Long id, AuthenticatedUser user, String authHeader) {
        Expense expense = findExpenseOrThrow(id);
        enforceOwnershipOrManagerRole(expense, user);

        String employeeName = "Unknown";
        String employeeEmail = "Unknown";

        try {
            UserDetailsResponse userDetails = authServiceClient.getUserById(expense.getEmployeeId(), authHeader);
            employeeName = userDetails.getName();
            employeeEmail = userDetails.getEmail();
        } catch (FeignException.NotFound e) {
            log.warn("User not found in Auth Service for employeeId={}", expense.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to fetch user details for employeeId={}: {}", expense.getEmployeeId(), e.getMessage());
        }

        return ExpenseWithUserResponse.builder()
                .id(expense.getId())
                .employeeId(expense.getEmployeeId())
                .employeeName(employeeName)
                .employeeEmail(employeeEmail)
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .status(expense.getStatus())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    private ExpenseSummaryResponse buildSummaryFromDatabase() {
        long total = expenseRepository.count();
        long pending = expenseRepository.countByStatus(ExpenseStatus.PENDING);
        long approved = expenseRepository.countByStatus(ExpenseStatus.APPROVED);
        long rejected = expenseRepository.countByStatus(ExpenseStatus.REJECTED);
        var totalAmount = expenseRepository.sumAllAmounts();

        return ExpenseSummaryResponse.builder()
                .totalExpenses(total)
                .pendingExpenses(pending)
                .approvedExpenses(approved)
                .rejectedExpenses(rejected)
                .totalAmount(totalAmount)
                .build();
    }

    private void invalidateSummaryCache() {
        redisTemplate.delete(SUMMARY_CACHE_KEY);
        log.info("Expense summary cache invalidated");
    }

    private Expense findExpenseOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
    }

    private void enforceOwnershipOrManagerRole(Expense expense, AuthenticatedUser user) {
        boolean isOwner = expense.getEmployeeId().equals(user.userId());
        boolean isManagerOrAdmin = user.role().equals("MANAGER") || user.role().equals("ADMIN");

        if (!isOwner && !isManagerOrAdmin) {
            throw new UnauthorizedException("You do not have permission to access this expense");
        }
    }

    private void requireManagerOrAdmin(AuthenticatedUser user) {
        if (!user.role().equals("MANAGER") && !user.role().equals("ADMIN")) {
            throw new UnauthorizedException("Only managers or admins can perform this action");
        }
    }

    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .employeeId(expense.getEmployeeId())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .status(expense.getStatus())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
