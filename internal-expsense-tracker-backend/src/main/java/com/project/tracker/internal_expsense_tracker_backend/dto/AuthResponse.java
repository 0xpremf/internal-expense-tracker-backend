package com.project.tracker.internal_expsense_tracker_backend.dto;

import com.project.tracker.internal_expsense_tracker_backend.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long id;
    private String name;
    private String email;
    private Role role;
    private Long departmentId;
    private String departmentName;
}
