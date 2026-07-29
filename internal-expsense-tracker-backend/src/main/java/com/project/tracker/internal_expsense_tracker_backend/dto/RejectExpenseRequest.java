package com.project.tracker.internal_expsense_tracker_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectExpenseRequest {
    @NotBlank (message = "Rejection reason is required")
    private String rejectionReason;
}
