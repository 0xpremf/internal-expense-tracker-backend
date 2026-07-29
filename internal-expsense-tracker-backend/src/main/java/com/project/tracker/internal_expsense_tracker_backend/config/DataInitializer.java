package com.project.tracker.internal_expsense_tracker_backend.config;

import com.project.tracker.internal_expsense_tracker_backend.Repo.DeptRepo;
import com.project.tracker.internal_expsense_tracker_backend.Repo.ExpenseRepo;
import com.project.tracker.internal_expsense_tracker_backend.Repo.UserRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.*;
import com.project.tracker.internal_expsense_tracker_backend.service.RiskAnalysisEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile ("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DeptRepo departmentRepository;
    private final UserRepo userRepository;
    private final ExpenseRepo expenseRepository;
    private final PasswordEncoder passwordEncoder;
    private final RiskAnalysisEngine riskAnalysisEngine;

    @Override
    public void run(String @NonNull ... args) throws Exception {
        log.info("========== DataInitializer Started ==========");
        if (departmentRepository.count() > 0) {
            return; // Data already initialized, skip
        }

        // 1. Seed Departments
        Department engineering = departmentRepository.save(Department.builder()
                .name("Engineering")
                .code("ENG")
                .monthlyBudget(new BigDecimal("10000.00"))
                .build());

        Department sales = departmentRepository.save(Department.builder()
                .name("Sales")
                .code("SLS")
                .monthlyBudget(new BigDecimal("15000.00"))
                .build());

        Department finance = departmentRepository.save(Department.builder()
                .name("Finance")
                .code("FIN")
                .monthlyBudget(new BigDecimal("20000.00"))
                .build());

        // 2. Seed Users (password is "password123" for all)
        User empAlice = userRepository.save(User.builder()
                .username("Alice Smith")
                .email("alice@company.com")
                .password_hash(passwordEncoder.encode("password123"))
                .role(Role.EMPLOYEE)
                .department(engineering)
                .build());

        User empBob = userRepository.save(User.builder()
                .username("Bob Jones")
                .email("bob@company.com")
                .password_hash(passwordEncoder.encode("password123"))
                .role(Role.EMPLOYEE)
                .department(sales)
                .build());

        userRepository.save(User.builder()
                .username("Charlie Miller")
                .email("charlie@company.com")
                .password_hash(passwordEncoder.encode("password123"))
                .role(Role.MANAGER)
                .department(engineering)
                .build());

        User empDiana = userRepository.save(User.builder()
                .username("Diana Ross")
                .email("diana@company.com")
                .password_hash(passwordEncoder.encode("password123"))
                .role(Role.MANAGER)
                .department(sales)
                .build());

        userRepository.save(User.builder()
                .username("Edward Finance")
                .email("edward@company.com")
                .password_hash(passwordEncoder.encode("password123"))
                .role(Role.FINANCE)
                .department(finance)
                .build());

        // 3. Seed Sample Expenses across different statuses
        Expense exp1 = expenseRepository.save(Expense.builder()
                .title("AWS Cloud Services")
                .amount(new BigDecimal("1250.00"))
                .currency("USD")
                .category(ExpenseCategory.SOFTWARE)
                .status(ExpenseStatus.SUBMITTED)
                .author(empAlice)
                .department(engineering)
                .notes("Monthly cloud infrastructure cost")
                .build());
        riskAnalysisEngine.analyzeExpense(exp1);
        expenseRepository.save(exp1);

        Expense exp2 = expenseRepository.save(Expense.builder()
                .title("Team Dinner")
                .amount(new BigDecimal("350.00"))
                .currency("USD")
                .category(ExpenseCategory.MEALS)
                .status(ExpenseStatus.APPROVED)
                .author(empAlice)
                .department(engineering)
                .notes("Post-release celebration meal")
                .build());
        riskAnalysisEngine.analyzeExpense(exp2);
        expenseRepository.save(exp2);

        Expense exp3 = expenseRepository.save(Expense.builder()
                .title("Client Flight Tickets")
                .amount(new BigDecimal("1500.00"))
                .currency("USD")
                .category(ExpenseCategory.TRAVEL)
                .status(ExpenseStatus.DRAFT)
                .author(empBob)
                .department(sales)
                .notes("Round trip to New York for client pitch")
                .build());
        riskAnalysisEngine.analyzeExpense(exp3);
        expenseRepository.save(exp3);

        // High-risk round-number expense example
        Expense exp4 = expenseRepository.save(Expense.builder()
                .title("Laptop Accessories Purchase")
                .amount(new BigDecimal("2500.00"))
                .currency("USD")
                .category(ExpenseCategory.EQUIPMENT)
                .status(ExpenseStatus.SUBMITTED)
                .author(empAlice)
                .department(engineering)
                .notes("New monitors, keyboard and dock")
                .build());
        riskAnalysisEngine.analyzeExpense(exp4);
        expenseRepository.save(exp4);

        log.info("<===============Data Initialized===============>");
    }

}
