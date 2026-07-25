package com.project.tracker.internal_expsense_tracker_backend.dto;

import com.project.tracker.internal_expsense_tracker_backend.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank (message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email (message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size (min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotNull (message = "Role is required")
    private Role role;

    @NotNull(message = "Department ID is required")
    private Long departmentId;
}
