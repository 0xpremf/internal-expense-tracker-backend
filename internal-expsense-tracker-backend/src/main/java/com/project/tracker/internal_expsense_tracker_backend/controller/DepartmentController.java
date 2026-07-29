package com.project.tracker.internal_expsense_tracker_backend.controller;

import com.project.tracker.internal_expsense_tracker_backend.Repo.DeptRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.Department;
import com.project.tracker.internal_expsense_tracker_backend.dto.DepartmentBudgetResponse;
import com.project.tracker.internal_expsense_tracker_backend.dto.UpdateBudgetRequest;
import com.project.tracker.internal_expsense_tracker_backend.service.AuthService;
import com.project.tracker.internal_expsense_tracker_backend.service.DepartmentBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping ("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final AuthService authService;
    private final DeptRepo departmentRepository;
    private final DepartmentBudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @GetMapping("/{id}/budget")
    public ResponseEntity<DepartmentBudgetResponse> getDepartmentBudget(
            @PathVariable Long id,
            @RequestParam (required = false) String month) {

        YearMonth ym = month != null ? YearMonth.parse(month) : YearMonth.now();
        DepartmentBudgetResponse budgetResponse = budgetService.getDepartmentBudgetStatus(id, ym);
        return ResponseEntity.ok(budgetResponse);
    }

    @PutMapping("/{id}/budget")
    public ResponseEntity<Department> updateDepartmentBudget(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBudgetRequest request) {

        Department updated = budgetService.updateDepartmentBudget(id, request.getMonthlyBudget(), authService.getLoggedInUser());
        return ResponseEntity.ok(updated);
    }
}
