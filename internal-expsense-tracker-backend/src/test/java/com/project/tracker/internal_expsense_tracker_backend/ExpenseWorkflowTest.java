package com.project.tracker.internal_expsense_tracker_backend;


import com.project.tracker.internal_expsense_tracker_backend.Repo.DeptRepo;
import com.project.tracker.internal_expsense_tracker_backend.Repo.ExpenseRepo;
import com.project.tracker.internal_expsense_tracker_backend.Repo.UserRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.*;
import com.project.tracker.internal_expsense_tracker_backend.dto.ApprovalResultResponse;
import com.project.tracker.internal_expsense_tracker_backend.dto.CreateExpenseRequest;
import com.project.tracker.internal_expsense_tracker_backend.dto.ExpenseResponse;
import com.project.tracker.internal_expsense_tracker_backend.dto.PagedEnvelope;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.ForbiddenActionException;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.InvalidStateTransitionException;
import com.project.tracker.internal_expsense_tracker_backend.service.ExpenseService;
import com.project.tracker.internal_expsense_tracker_backend.service.ExpenseWorkflowService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.util.AssertionErrors.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class ExpenseWorkflowTest {
    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseWorkflowService workflowService;

    @Autowired
    private ExpenseRepo expenseRepository;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private DeptRepo departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Department engDept;
    private Department salesDept;
    private User empAlice;
    private User mgrCharlie;
    private User mgrDianaSales;
    @Autowired
    private DeptRepo deptRepo;


    @BeforeEach
    void setUp(){

        expenseRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        engDept = departmentRepository.save(Department.builder()
                .name("Engineering")
                .code("ENG")
                .monthlyBudget(new BigDecimal("5000.00"))
                .build());

        salesDept = departmentRepository.save(Department.builder()
                .name("Sales")
                .monthlyBudget(new BigDecimal("8000"))
                .code("SLS")
                .build());

        empAlice = userRepository.save(User.builder()
                .username("Alice")
                .email("alice@company.com")
                .password_hash(passwordEncoder.encode("pass"))
                .role(Role.EMPLOYEE)
                .department(engDept)
                .build());

        mgrCharlie = userRepository.save(User.builder()
                .username("Charlie (ENG Mgr)")
                .email("charlie@test.com")
                .password_hash(passwordEncoder.encode("pass"))
                .role(Role.MANAGER)
                .department(engDept)
                .build());

        mgrDianaSales = userRepository.save(User.builder()
                .username("Diana (Sales Mgr)")
                .email("diana@test.com")
                .password_hash(passwordEncoder.encode("pass"))
                .role(Role.MANAGER)
                .department(salesDept)
                .build());
    }

    @Test
    @DisplayName("")
    void testSuccessfulApprovalWorkflow(){
        CreateExpenseRequest createReq = new CreateExpenseRequest();
        createReq.setTitle("IDE License");
        createReq.setAmount(new BigDecimal("299.99"));
        createReq.setCurrency("USD");
        createReq.setCategory(ExpenseCategory.SOFTWARE);
        createReq.setSubmitNow(false);

        ExpenseResponse createResponse = expenseService.createExpense(createReq, empAlice);
        assertEquals("Done",ExpenseStatus.DRAFT, createResponse.getStatus());

        Expense submitted = workflowService.submitExpense(createResponse.getId(), empAlice);
        Assertions.assertEquals(ExpenseStatus.SUBMITTED, submitted.getStatus());
        assertNotNull(submitted.getRiskScore());

        ApprovalResultResponse approvedResult = workflowService.approveExpense(createResponse.getId(), mgrCharlie);
        Assertions.assertEquals(ExpenseStatus.APPROVED, approvedResult.getExpense().getStatus());
        assertNull(approvedResult.getWarning());



    }
    @Test
    @DisplayName("Rejection Workflow: Submitted -> Rejected -> Reopen to Draft")
    void testRejectionAndReopenWorkflow() {
        CreateExpenseRequest createReq = new CreateExpenseRequest();
        createReq.setTitle("Personal Lunch");
        createReq.setAmount(new BigDecimal("80.00"));
        createReq.setCurrency("USD");
        createReq.setCategory(ExpenseCategory.MEALS);
        createReq.setSubmitNow(true);

        ExpenseResponse created = expenseService.createExpense(createReq, empAlice);
        Assertions.assertEquals(ExpenseStatus.SUBMITTED, created.getStatus());

        // Reject expense
        Expense rejected = workflowService.rejectExpense(created.getId(), "Non-compliant expense", mgrCharlie);
        Assertions.assertEquals(ExpenseStatus.REJECTED, rejected.getStatus());
        Assertions.assertEquals("Non-compliant expense", rejected.getRejectionReason());

        // Reopen expense back to draft
        Expense reopened = workflowService.reopenExpense(created.getId(), empAlice);
        Assertions.assertEquals(ExpenseStatus.DRAFT, reopened.getStatus());
    }

    @Test
    @DisplayName("Invalid State Transition: Cannot approve a DRAFT expense directly - Must throw InvalidStateTransitionException (422)")
    void testInvalidTransitionThrowsException() {
        CreateExpenseRequest createReq = new CreateExpenseRequest();
        createReq.setTitle("Draft Monitor");
        createReq.setAmount(new BigDecimal("400.00"));
        createReq.setCurrency("USD");
        createReq.setCategory(ExpenseCategory.EQUIPMENT);
        createReq.setSubmitNow(false);

        ExpenseResponse created = expenseService.createExpense(createReq, empAlice);

        assertThrows(InvalidStateTransitionException.class, () -> {
            workflowService.approveExpense(created.getId(), mgrCharlie);
        });
    }

    @Test
    @DisplayName("Manager Conflict of Interest: Manager cannot approve their own expense")
    void testManagerSelfApprovalForbidden() {
        User mgrSelfEmp = userRepository.save(User.builder()
                .username("Self Manager")
                .email("self.mgr@test.com")
                .password_hash(passwordEncoder.encode("pass"))
                .role(Role.MANAGER)
                .department(engDept)
                .build());

        CreateExpenseRequest createReq = new CreateExpenseRequest();
        createReq.setTitle("Manager Travel");
        createReq.setAmount(new BigDecimal("500.00"));
        createReq.setCurrency("USD");
        createReq.setCategory(ExpenseCategory.TRAVEL);
        createReq.setSubmitNow(true);

        ExpenseResponse created = expenseService.createExpense(createReq, mgrSelfEmp);

        assertThrows(ForbiddenActionException.class, () -> {
            workflowService.approveExpense(created.getId(), mgrSelfEmp);
        });
    }

    @Test
    @DisplayName("Department Scope Violation: Manager cannot approve expense from another department")
    void testCrossDepartmentApprovalForbidden() {
        CreateExpenseRequest createReq = new CreateExpenseRequest();
        createReq.setTitle("Engineering Server");
        createReq.setAmount(new BigDecimal("1200.00"));
        createReq.setCurrency("USD");
        createReq.setCategory(ExpenseCategory.EQUIPMENT);
        createReq.setSubmitNow(true);

        ExpenseResponse created = expenseService.createExpense(createReq, empAlice);

        // Sales Manager attempts to approve Engineering expense
        assertThrows(ForbiddenActionException.class, () -> {
            workflowService.approveExpense(created.getId(), mgrDianaSales);
        });
    }

    @Test
    @DisplayName("Expense Filtering Envelope - Envelope matches { total, page, limit, data }")
    void testListExpensesFilteringEnvelope() {
        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setTitle("Flight Ticket");
        req.setAmount(new BigDecimal("600.00"));
        req.setCurrency("USD");
        req.setCategory(ExpenseCategory.TRAVEL);
        req.setSubmitNow(true);
        expenseService.createExpense(req, empAlice);

        PagedEnvelope<ExpenseResponse> envelope = expenseService.listExpenses(
                null, null, ExpenseCategory.TRAVEL, null, null, null, null,
                1, 20, "createdAt", "desc", empAlice);

        assertNotNull(envelope);
        Assertions.assertEquals(1, envelope.getTotal());
        Assertions.assertEquals(1, envelope.getPage());
        Assertions.assertEquals(20, envelope.getLimit());
        Assertions.assertEquals(1, envelope.getData().size());
        Assertions.assertEquals("Flight Ticket", envelope.getData().get(0).getTitle());
    }



}
