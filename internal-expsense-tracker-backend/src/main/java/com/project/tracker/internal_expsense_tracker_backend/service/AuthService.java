package com.project.tracker.internal_expsense_tracker_backend.service;

import com.project.tracker.internal_expsense_tracker_backend.Repo.DeptRepo;
import com.project.tracker.internal_expsense_tracker_backend.Repo.UserRepo;
import com.project.tracker.internal_expsense_tracker_backend.domain.Department;
import com.project.tracker.internal_expsense_tracker_backend.domain.User;
import com.project.tracker.internal_expsense_tracker_backend.dto.AuthResponse;
import com.project.tracker.internal_expsense_tracker_backend.dto.LoginRequest;
import com.project.tracker.internal_expsense_tracker_backend.dto.RegisterRequest;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.ResourceNotFoundException;
import com.project.tracker.internal_expsense_tracker_backend.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.project.tracker.internal_expsense_tracker_backend.exceptions.BadRequestException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final DeptRepo deptRepo;
    private final UserRepo userRepo;


    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .build();
    }
    @Transactional
    public AuthResponse register(@Valid RegisterRequest registerRequest) throws BadRequestException {
        if(userRepo.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email already exists");

        }


        Department department = deptRepo.findById(registerRequest.getDepartmentId()).orElseThrow(() -> new BadRequestException("Department not found"));

        User user = User.builder()
                .username(registerRequest.getName())
                .email(registerRequest.getEmail().toLowerCase())
                .password_hash(passwordEncoder.encode(registerRequest.getPassword()))
                .role(registerRequest.getRole())
                .department(department)
                .build();
        user = userRepo.save(user);
        String token = jwtUtil.createToken(user.getEmail());
        return buildAuthResponse(user, token);
    }

    @Transactional (readOnly = true)
    public AuthResponse login(@Valid LoginRequest loginRequest) {
        String email = loginRequest.getEmail().toLowerCase();

        User user = userRepo.findByEmail(email).orElseThrow(()-> new BadCredentialsException("Invalid Email Id."));

        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword_hash())) {
            throw new BadCredentialsException("Invalid Password.");
        }

        String token = jwtUtil.createToken(user.getEmail());
        return buildAuthResponse(user, token);

    }
    @Transactional (readOnly = true)
    public User getLoggedInUser() {
        String email = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return userRepo.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("User Not Found"));
    }



}
