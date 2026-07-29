package com.project.tracker.internal_expsense_tracker_backend.controller;


import com.project.tracker.internal_expsense_tracker_backend.domain.ExpenseCategory;
import com.project.tracker.internal_expsense_tracker_backend.domain.ExpenseStatus;
import com.project.tracker.internal_expsense_tracker_backend.dto.*;
import com.project.tracker.internal_expsense_tracker_backend.service.AuthService;
import com.project.tracker.internal_expsense_tracker_backend.service.ExpenseService;
import com.project.tracker.internal_expsense_tracker_backend.service.ExpenseWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final AuthService authService;
    private final ExpenseService expenseService;
    private final ExpenseWorkflowService workflowService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        ExpenseResponse response = expenseService.createExpense(request, authService.getLoggedInUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedEnvelope<ExpenseResponse>> listExpenses(
            @RequestParam(required = false, name = "start_date") String startDate,
            @RequestParam(required = false, name = "end_date") String endDate,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false, name = "min_amount") BigDecimal minAmount,
            @RequestParam(required = false, name = "max_amount") BigDecimal maxAmount,
            @RequestParam(required = false, name = "department_id") Long departmentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {

        PagedEnvelope<ExpenseResponse> envelope = expenseService.listExpenses(
                startDate, endDate, category, status, minAmount, maxAmount, departmentId,
                page, limit, sort, order, authService.getLoggedInUser());

        return ResponseEntity.ok(envelope);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id) {
        ExpenseResponse response = expenseService.getExpenseById(id, authService.getLoggedInUser());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExpenseRequest request) {
        ExpenseResponse response = expenseService.updateExpense(id, request, authService.getLoggedInUser());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id, authService.getLoggedInUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ExpenseResponse> submitExpense(@PathVariable Long id) {
        var expense = workflowService.submitExpense(id, authService.getLoggedInUser());
        return ResponseEntity.ok(workflowService.mapToResponseDto(expense));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalResultResponse> approveExpense(@PathVariable Long id) {
        ApprovalResultResponse response = workflowService.approveExpense(id, authService.getLoggedInUser());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ExpenseResponse> rejectExpense(
            @PathVariable Long id,
            @Valid @RequestBody RejectExpenseRequest request) {
        var expense = workflowService.rejectExpense(id, request.getRejectionReason(), authService.getLoggedInUser());
        return ResponseEntity.ok(workflowService.mapToResponseDto(expense));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<ExpenseResponse> reopenExpense(@PathVariable Long id) {
        var expense = workflowService.reopenExpense(id, authService.getLoggedInUser());
        return ResponseEntity.ok(workflowService.mapToResponseDto(expense));
    }
}
