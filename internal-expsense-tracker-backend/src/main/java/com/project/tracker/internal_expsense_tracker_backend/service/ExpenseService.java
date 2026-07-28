package com.project.tracker.internal_expsense_tracker_backend.service;


import com.project.tracker.internal_expsense_tracker_backend.Repo.ExpenseRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.*;
import com.project.tracker.internal_expsense_tracker_backend.dto.CreateExpenseRequest;
import com.project.tracker.internal_expsense_tracker_backend.dto.ExpenseResponse;
import com.project.tracker.internal_expsense_tracker_backend.dto.PagedEnvelope;
import com.project.tracker.internal_expsense_tracker_backend.dto.UpdateExpenseRequest;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.ForbiddenActionException;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.InvalidStateTransitionException;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepo expenseRepository;
    private final RiskAnalysisEngine riskAnalysisEngine;
    private final ExpenseWorkflowService workflowService;

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request, User author) {
        checkEmployeeOrManager(author);

        Expense expense = Expense.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .category(request.getCategory())
                .status(ExpenseStatus.DRAFT)
                .author(author)
                .department(author.getDepartment())
                .receiptUrl(request.getReceiptUrl())
                .notes(request.getNotes())
                .build();

        expense = expenseRepository.save(expense);

        if (request.isSubmitNow()) {
            expense = workflowService.submitExpense(expense.getId(), author);
        } else {
            // Run initial risk score assessment for draft
            riskAnalysisEngine.analyzeExpense(expense);
            expense = expenseRepository.save(expense);
        }

        return workflowService.mapToResponseDto(expense);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id, User currentUser) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        validateViewAccess(expense, currentUser);

        return workflowService.mapToResponseDto(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, UpdateExpenseRequest request, User currentUser) {
        checkEmployeeOrManager(currentUser);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        if (!expense.getAuthor().getId().equals(currentUser.getId())) {
            throw new ForbiddenActionException("Only the expense author can edit an expense.");
        }

        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot edit expense with status '%s'. Only draft expenses can be edited.", expense.getStatus().toValue()));
        }

        if (StringUtils.hasText(request.getTitle())) expense.setTitle(request.getTitle());
        if (request.getAmount() != null) expense.setAmount(request.getAmount());
        if (StringUtils.hasText(request.getCurrency())) expense.setCurrency(request.getCurrency().toUpperCase());
        if (request.getCategory() != null) expense.setCategory(request.getCategory());
        if (request.getReceiptUrl() != null) expense.setReceiptUrl(request.getReceiptUrl());
        if (request.getNotes() != null) expense.setNotes(request.getNotes());

        riskAnalysisEngine.analyzeExpense(expense);
        expense = expenseRepository.save(expense);

        return workflowService.mapToResponseDto(expense);
    }

    @Transactional
    public void deleteExpense(Long id, User currentUser) {
        checkEmployeeOrManager(currentUser);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        if (!expense.getAuthor().getId().equals(currentUser.getId())) {
            throw new ForbiddenActionException("Only the expense author can delete an expense.");
        }

        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot delete expense with status '%s'. Only draft expenses can be deleted.", expense.getStatus().toValue()));
        }

        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public PagedEnvelope<ExpenseResponse> listExpenses(
            String startDateStr,
            String endDateStr,
            ExpenseCategory category,
            ExpenseStatus status,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Long departmentId,
            int page,
            int limit,
            String sortField,
            String sortOrder,
            User currentUser) {

        // Enforce pagination limits (default 20, max 100)
        int effectiveLimit = Math.clamp(limit, 1, 100);
        int effectivePage = Math.max(page, 1); // 1-indexed for client API
        int zeroBasedPage = effectivePage - 1;

        // Sorting
        String field = StringUtils.hasText(sortField) ? sortField : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(zeroBasedPage, effectiveLimit, Sort.by(direction, field));

        // Build JPA Specification for dynamic filtering
        Specification<Expense> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Role-based scoping
            if (currentUser.getRole() == Role.EMPLOYEE) {
                predicates.add(cb.equal(root.get("author").get("id"), currentUser.getId()));
            } else if (currentUser.getRole() == Role.MANAGER) {
                predicates.add(cb.equal(root.get("department").get("id"), currentUser.getDepartment().getId()));
            } else if (currentUser.getRole() == Role.FINANCE) {
                // Finance can view all, or filter by specific department
                if (departmentId != null) {
                    predicates.add(cb.equal(root.get("department").get("id"), departmentId));
                }
            }

            // 2. Date Range
            if (StringUtils.hasText(startDateStr)) {
                LocalDateTime start = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_DATE).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (StringUtils.hasText(endDateStr)) {
                LocalDateTime end = LocalDate.parse(endDateStr, DateTimeFormatter.ISO_DATE).atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }

            // 3. Category
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            // 4. Status
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 5. Amount Range
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Expense> pageResult = expenseRepository.findAll(spec, pageable);

        List<ExpenseResponse> dtoList = pageResult.getContent().stream()
                .map(workflowService::mapToResponseDto)
                .collect(Collectors.toList());

        return PagedEnvelope.<ExpenseResponse>builder()
                .total(pageResult.getTotalElements())
                .page(effectivePage)
                .limit(effectiveLimit)
                .data(dtoList)
                .build();
    }

    private void validateViewAccess(Expense expense, User user) {
        if (user.getRole() == Role.EMPLOYEE && !expense.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenActionException("Employees can only view their own expense history.");
        }
        if (user.getRole() == Role.MANAGER && !expense.getDepartment().getId().equals(user.getDepartment().getId())) {
            throw new ForbiddenActionException("Managers can only view expenses within their own department.");
        }
    }

    private void checkEmployeeOrManager(User user) {
        if (user.getRole() != Role.EMPLOYEE && user.getRole() != Role.MANAGER) {
            throw new ForbiddenActionException("Only employees and managers can do this.");
        }
    }
}
