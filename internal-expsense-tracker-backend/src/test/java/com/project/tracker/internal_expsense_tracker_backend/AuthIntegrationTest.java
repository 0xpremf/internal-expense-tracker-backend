package com.project.tracker.internal_expsense_tracker_backend;

import com.project.tracker.internal_expsense_tracker_backend.Repo.DeptRepo;
import com.project.tracker.internal_expsense_tracker_backend.Repo.ExpenseRepo;
import com.project.tracker.internal_expsense_tracker_backend.Repo.UserRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.Department;
import com.project.tracker.internal_expsense_tracker_backend.domain.Role;
import com.project.tracker.internal_expsense_tracker_backend.dto.AuthResponse;
import com.project.tracker.internal_expsense_tracker_backend.dto.LoginRequest;
import com.project.tracker.internal_expsense_tracker_backend.dto.RegisterRequest;
import com.project.tracker.internal_expsense_tracker_backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class AuthIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private ExpenseRepo expenseRepository;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private DeptRepo departmentRepository;

    private Department department;

    @BeforeEach
    void setUp() {
        // Delete in FK-safe order: expenses -> users -> departments
        expenseRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        department = departmentRepository.save(Department.builder()
                .name("Test Department")
                .code("TEST_DEPT")
                .build());
    }

    @Test
    @DisplayName ("Should register a new employee successfully and issue a JWT token")
    void testRegisterUser() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john.doe@test.com");
        request.setPassword("password123");
        request.setRole(Role.EMPLOYEE);
        request.setDepartmentId(department.getId());

        AuthResponse response = authService.register(request);

        assertNotNull(response.getToken());
        assertEquals("John Doe", response.getUsername());
        assertEquals("john.doe@test.com", response.getEmail());
        assertEquals(Role.EMPLOYEE, response.getRole());
        assertEquals(department.getId(), response.getDepartmentId());
    }

    @Test
    @DisplayName("Should authenticate user and return a valid JWT on correct login credentials")
    void testLoginUser() {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setName("Jane Doe");
        registerReq.setEmail("jane.doe@test.com");
        registerReq.setPassword("password123");
        registerReq.setRole(Role.MANAGER);
        registerReq.setDepartmentId(department.getId());
        authService.register(registerReq);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("jane.doe@test.com");
        loginReq.setPassword("password123");

        AuthResponse response = authService.login(loginReq);

        assertNotNull(response.getToken());
        assertEquals("Jane Doe", response.getUsername());
        assertEquals(Role.MANAGER, response.getRole());
        assertEquals(department.getId(), response.getDepartmentId());
    }
}
