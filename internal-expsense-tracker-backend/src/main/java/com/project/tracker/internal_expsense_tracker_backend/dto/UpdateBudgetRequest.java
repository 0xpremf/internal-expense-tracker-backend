package com.project.tracker.internal_expsense_tracker_backend.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateBudgetRequest {
    @NotNull (message = "Monthly budget is required")
    @DecimalMin (value = "0.00", message = "Monthly budget cannot be negative")
    private BigDecimal monthlyBudget;
}

