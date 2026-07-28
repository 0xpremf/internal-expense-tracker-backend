package com.project.tracker.internal_expsense_tracker_backend.dto;

import com.project.tracker.internal_expsense_tracker_backend.domain.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateExpenseRequest {
    private String title;

    @DecimalMin (value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size (min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    private ExpenseCategory category;

    private String receiptUrl;

    private String notes;
}
