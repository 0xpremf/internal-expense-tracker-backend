package com.project.tracker.internal_expsense_tracker_backend;


import com.project.tracker.internal_expsense_tracker_backend.Repo.DeptRepo;
import com.project.tracker.internal_expsense_tracker_backend.Repo.ExpenseRepo;
import com.project.tracker.internal_expsense_tracker_backend.Repo.UserRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.Department;
import com.project.tracker.internal_expsense_tracker_backend.domain.Role;
import com.project.tracker.internal_expsense_tracker_backend.domain.User;
import com.project.tracker.internal_expsense_tracker_backend.service.DepartmentBudgetService;
import com.project.tracker.internal_expsense_tracker_backend.service.ExpenseWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

@SpringBootTest
@ActiveProfiles("test")
public class ExpenseConcurrencyTest {
    @Autowired
    private ExpenseWorkflowService workflowService;

    @Autowired
    private DepartmentBudgetService budgetService;

    @Autowired
    private ExpenseRepo expenseRepository;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private DeptRepo departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Department engDepartment;
    private User employeeAlice;
    private User managerCharlie;
    private User managerDiana;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        engDepartment = departmentRepository.save(Department.builder()
                .name("Engineering Concurrency Test")
                .code("ENG_CONC")
                .monthlyBudget(new BigDecimal("500.00")) // Small budget to trigger warnings easily
                .build());

        employeeAlice = userRepository.save(User.builder()
                .username("Alice Concurrent")
                .email("alice.conc@test.com")
                .password_hash(passwordEncoder.encode("pass"))
                .role(Role.EMPLOYEE)
                .department(engDepartment)
                .build());

        managerCharlie = userRepository.save(User.builder()
                .username("Charlie Manager 1")
                .email("charlie.m1@test.com")
                .password_hash(passwordEncoder.encode("pass"))
                .role(Role.MANAGER)
                .department(engDepartment)
                .build());

        managerDiana = userRepository.save(User.builder()
                .username("Diana Manager 2")
                .email("diana.m2@test.com")
                .password_hash(passwordEncoder.encode("pass"))
                .role(Role.MANAGER)
                .department(engDepartment)
                .build());
    }
}
