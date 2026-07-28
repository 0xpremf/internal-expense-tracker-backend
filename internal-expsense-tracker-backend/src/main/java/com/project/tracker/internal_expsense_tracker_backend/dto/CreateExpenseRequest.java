package com.project.tracker.internal_expsense_tracker_backend.dto;


import com.project.tracker.internal_expsense_tracker_backend.domain.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateExpenseRequest {
    @NotBlank (message = "Title is required")
    private String title;

    @NotNull (message = "Amount is required")
    @DecimalMin (value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size (min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g. USD, EUR)")
    private String currency;

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    private boolean submitNow = false;

    private String receiptUrl;

    private String notes;
}

