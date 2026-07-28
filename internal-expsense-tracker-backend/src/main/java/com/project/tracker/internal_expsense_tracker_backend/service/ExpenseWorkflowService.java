package com.project.tracker.internal_expsense_tracker_backend.service;

import com.project.tracker.internal_expsense_tracker_backend.Repo.ExpenseRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.*;
import com.project.tracker.internal_expsense_tracker_backend.dto.ApprovalResultResponse;
import com.project.tracker.internal_expsense_tracker_backend.dto.DepartmentBudgetResponse;
import com.project.tracker.internal_expsense_tracker_backend.dto.ExpenseResponse;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.ForbiddenActionException;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.InvalidStateTransitionException;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ExpenseWorkflowService {

    private final ExpenseRepo expenseRepository;
    private final RiskAnalysisEngine riskAnalysisEngine;
    private final DepartmentBudgetService budgetService;

    @Transactional
    public Expense submitExpense(Long expenseId, User user) {
        checkEmployeeOrManager(user);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + expenseId));

        validateAuthorPermission(expense, user, "submit");

        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot submit expense with status '%s'. Only expenses in 'draft' status can be submitted.",
                            expense.getStatus().toValue()));
        }

        expense.setStatus(ExpenseStatus.SUBMITTED);

        // Run AI risk analysis engine on submission
        riskAnalysisEngine.analyzeExpense(expense);

        return expenseRepository.save(expense);
    }

    @Transactional
    public ApprovalResultResponse approveExpense(Long expenseId, User manager) {
        Expense expense = expenseRepository.findByIdWithPessimisticLock(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + expenseId));

        validateManagerPermission(expense, manager, "approve");

        if (expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot approve expense with status '%s'. Only expenses in 'submitted' status can be approved.",
                            expense.getStatus().toValue()));
        }

        // Calculate budget impact before marking as approved
        Department dept = expense.getDepartment();
        YearMonth currentMonth = YearMonth.now();
        DepartmentBudgetResponse budgetStatusBefore = budgetService.getDepartmentBudgetStatus(dept.getId(), currentMonth);

        expense.setStatus(ExpenseStatus.APPROVED);
        Expense savedExpense = expenseRepository.save(expense);

        // Check if approval exceeds monthly budget cap
        String warning = null;
        if (dept.getMonthlyBudget() != null && dept.getMonthlyBudget().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newSpent = budgetStatusBefore.getSpent().add(expense.getAmount());
            if (newSpent.compareTo(dept.getMonthlyBudget()) > 0) {
                warning = String.format("WARNING: Approval pushes %s department spend (%s %.2f) over monthly budget cap (%s %.2f).",
                        dept.getName(), expense.getCurrency(), newSpent, expense.getCurrency(), dept.getMonthlyBudget());
            }
        }

        ExpenseResponse responseDto = mapToResponseDto(savedExpense);
        return ApprovalResultResponse.builder()
                .expense(responseDto)
                .warning(warning)
                .build();
    }

    @Transactional
    public Expense rejectExpense(Long expenseId, String rejectionReason, User manager) {
        if (!StringUtils.hasText(rejectionReason)) {
            throw new InvalidStateTransitionException("Rejection reason is mandatory when rejecting an expense.");
        }

        Expense expense = expenseRepository.findByIdWithPessimisticLock(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + expenseId));

        validateManagerPermission(expense, manager, "reject");

        if (expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot reject expense with status '%s'. Only expenses in 'submitted' status can be rejected.",
                            expense.getStatus().toValue()));
        }

        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setRejectionReason(rejectionReason);

        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense reopenExpense(Long expenseId, User user) {
        checkEmployeeOrManager(user);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + expenseId));

        validateAuthorPermission(expense, user, "reopen");

        if (expense.getStatus() != ExpenseStatus.REJECTED) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot reopen expense with status '%s'. Only expenses in 'rejected' status can be reopened to draft.",
                            expense.getStatus().toValue()));
        }

        expense.setStatus(ExpenseStatus.DRAFT);
        return expenseRepository.save(expense);
    }

    private void validateAuthorPermission(Expense expense, User user, String action) {
        if (!expense.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenActionException("Only the author can " + action + " this expense.");
        }
    }

    private void validateManagerPermission(Expense expense, User manager, String action) {
        if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.FINANCE) {
            throw new ForbiddenActionException("Only managers or finance can approve/reject expenses.");
        }

        if (expense.getAuthor().getId().equals(manager.getId())) {
            throw new ForbiddenActionException("You cannot " + action + " your own expense.");
        }

        if (manager.getRole() == Role.MANAGER && !expense.getDepartment().getId().equals(manager.getDepartment().getId())) {
            throw new ForbiddenActionException("Managers can only " + action + " expenses in their department.");
        }
    }

    private void checkEmployeeOrManager(User user) {
        if (user.getRole() != Role.EMPLOYEE && user.getRole() != Role.MANAGER) {
            throw new ForbiddenActionException("Only employees and managers can do this.");
        }
    }

    public ExpenseResponse mapToResponseDto(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .currency(expense.getCurrency())
                .category(expense.getCategory())
                .status(expense.getStatus())
                .authorId(expense.getAuthor().getId())
                .authorName(expense.getAuthor().getUsername())
                .authorEmail(expense.getAuthor().getEmail())
                .departmentId(expense.getDepartment().getId())
                .departmentName(expense.getDepartment().getName())
                .receiptUrl(expense.getReceiptUrl())
                .notes(expense.getNotes())
                .rejectionReason(expense.getRejectionReason())
                .riskScore(expense.getRiskScore())
                .riskLevel(expense.getRiskLevel())
                .riskReasons(expense.getRiskReasons() != null ? new ArrayList<>(expense.getRiskReasons()) : new ArrayList<>())
                .analyzedAt(expense.getAnalyzedAt())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
