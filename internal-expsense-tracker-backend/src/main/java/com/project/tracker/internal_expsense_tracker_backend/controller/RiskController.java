package com.project.tracker.internal_expsense_tracker_backend.controller;

import com.project.tracker.internal_expsense_tracker_backend.Repo.ExpenseRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.Expense;
import com.project.tracker.internal_expsense_tracker_backend.domain.Role;
import com.project.tracker.internal_expsense_tracker_backend.domain.User;
import com.project.tracker.internal_expsense_tracker_backend.dto.ExpenseResponse;
import com.project.tracker.internal_expsense_tracker_backend.dto.PagedEnvelope;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.ForbiddenActionException;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.ResourceNotFoundException;
import com.project.tracker.internal_expsense_tracker_backend.service.AuthService;
import com.project.tracker.internal_expsense_tracker_backend.service.ExpenseWorkflowService;
import com.project.tracker.internal_expsense_tracker_backend.service.RiskAnalysisEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping ("/api/expenses")
@RequiredArgsConstructor
public class RiskController {

    private final AuthService authService;
    private final ExpenseRepo expenseRepository;
    private final RiskAnalysisEngine riskAnalysisEngine;
    private final ExpenseWorkflowService workflowService;

    @PostMapping ("/{id}/analyze")
    public ResponseEntity<ExpenseResponse> analyzeExpense(@PathVariable Long id) {
        checkManagerOrFinance(authService.getLoggedInUser());

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        riskAnalysisEngine.analyzeExpense(expense);
        expense = expenseRepository.save(expense);

        return ResponseEntity.ok(workflowService.mapToResponseDto(expense));
    }

    @GetMapping ("/flagged")
    public ResponseEntity<PagedEnvelope<ExpenseResponse>> getFlaggedExpenses(
            @RequestParam(defaultValue = "25") int threshold,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        User user = authService.getLoggedInUser();
        checkManagerOrFinance(user);

        int effectiveLimit = Math.clamp(limit, 1, 100);
        int effectivePage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(effectivePage - 1, effectiveLimit, Sort.by(Sort.Direction.DESC, "riskScore"));

        Page<Expense> pageResult;
        if (user.getRole() == Role.MANAGER) {
            pageResult = expenseRepository.findFlaggedExpensesByDepartment(user.getDepartment().getId(), threshold, pageable);
        } else {
            pageResult = expenseRepository.findFlaggedExpenses(threshold, pageable);
        }

        List<ExpenseResponse> data = pageResult.getContent().stream()
                .map(workflowService::mapToResponseDto)
                .toList();

        return ResponseEntity.ok(PagedEnvelope.<ExpenseResponse>builder()
                .total(pageResult.getTotalElements())
                .page(effectivePage)
                .limit(effectiveLimit)
                .data(data)
                .build());
    }

    @GetMapping("/{id}/risk")
    public ResponseEntity<Map<String, Object>> getRiskBreakdown(@PathVariable Long id) {
        checkManagerOrFinance(authService.getLoggedInUser());

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        if (expense.getRiskScore() == null) {
            riskAnalysisEngine.analyzeExpense(expense);
            expense = expenseRepository.save(expense);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("expenseId", expense.getId());
        response.put("title", expense.getTitle());
        response.put("amount", expense.getAmount());
        response.put("currency", expense.getCurrency());
        response.put("riskScore", expense.getRiskScore());
        response.put("riskLevel", expense.getRiskLevel());
        response.put("riskReasons", expense.getRiskReasons());
        response.put("analyzedAt", expense.getAnalyzedAt());

        return ResponseEntity.ok(response);
    }

    private void checkManagerOrFinance(User user) {
        if (user.getRole() != Role.MANAGER && user.getRole() != Role.FINANCE) {
            throw new ForbiddenActionException("Only managers or finance can access this.");
        }
    }
}

