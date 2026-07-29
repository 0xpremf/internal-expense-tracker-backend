package com.project.tracker.internal_expsense_tracker_backend.controller;


import com.project.tracker.internal_expsense_tracker_backend.dto.AuthResponse;
import com.project.tracker.internal_expsense_tracker_backend.dto.LoginRequest;
import com.project.tracker.internal_expsense_tracker_backend.dto.RegisterRequest;
import com.project.tracker.internal_expsense_tracker_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) throws BadRequestException {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerRequest));
    }

    @PostMapping("login")
    public  ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) throws BadRequestException {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(){
        var user= authService.getLoggedInUser();
        return ResponseEntity.ok(AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .build());
    }
}
