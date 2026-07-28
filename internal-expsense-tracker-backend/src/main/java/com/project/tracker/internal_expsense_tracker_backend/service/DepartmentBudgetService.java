package com.project.tracker.internal_expsense_tracker_backend.service;


import com.project.tracker.internal_expsense_tracker_backend.Repo.DeptRepo;
import com.project.tracker.internal_expsense_tracker_backend.Repo.ExpenseRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.Department;
import com.project.tracker.internal_expsense_tracker_backend.domain.Role;
import com.project.tracker.internal_expsense_tracker_backend.domain.User;
import com.project.tracker.internal_expsense_tracker_backend.dto.DepartmentBudgetResponse;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.ForbiddenActionException;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DepartmentBudgetService {
    
    private final DeptRepo deptRepo;
    private final ExpenseRepo expenseRepo;
    
    public DepartmentBudgetResponse getDepartmentBudgetStatus(Long deptId, YearMonth targetMonth) {
        Department dept = deptRepo.findById(deptId).orElseThrow(()-> new ResourceNotFoundException("Department not found"));
        
        if(targetMonth.equals(YearMonth.now())) {
            targetMonth = YearMonth.now();
        }

        LocalDateTime startOfMonth = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = targetMonth.atEndOfMonth().atTime(LocalTime.MAX);

        BigDecimal spent = expenseRepo.calculateApprovedDepartmentSpendForPeriod(
                deptId, startOfMonth, endOfMonth);
        if (spent == null) spent = BigDecimal.ZERO;

        BigDecimal budget = dept.getMonthlyBudget() != null ? dept.getMonthlyBudget() : BigDecimal.ZERO;
        BigDecimal remaining = budget.subtract(spent);
        boolean overBudget = budget.compareTo(BigDecimal.ZERO) > 0 && spent.compareTo(budget) > 0;

        return DepartmentBudgetResponse.builder()
                .departmentId(dept.getId())
                .departmentName(dept.getName())
                .month(targetMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .budget(budget)
                .spent(spent)
                .remaining(remaining)
                .overBudget(overBudget)
                .build();
    }

    @Transactional
    public Department updateDepartmentBudget(Long departmentId, BigDecimal newBudget, User user) {
        if (user.getRole() != Role.FINANCE && user.getRole() != Role.MANAGER) {
            throw new ForbiddenActionException("Only finance or managers can update budget.");
        }

        Department department = deptRepo.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + departmentId));
        department.setMonthlyBudget(newBudget);
        return deptRepo.save(department);
    }

}
