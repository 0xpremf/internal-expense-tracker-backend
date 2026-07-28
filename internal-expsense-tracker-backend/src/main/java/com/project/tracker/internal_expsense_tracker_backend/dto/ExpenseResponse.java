package com.project.tracker.internal_expsense_tracker_backend.dto;

import com.project.tracker.internal_expsense_tracker_backend.domain.ExpenseCategory;
import com.project.tracker.internal_expsense_tracker_backend.domain.ExpenseStatus;
import com.project.tracker.internal_expsense_tracker_backend.domain.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private String title;
    private BigDecimal amount;
    private String currency;
    private ExpenseCategory category;
    private ExpenseStatus status;

    private Long authorId;
    private String authorName;
    private String authorEmail;

    private Long departmentId;
    private String departmentName;

    private String receiptUrl;
    private String notes;
    private String rejectionReason;

    private Integer riskScore;
    private RiskLevel riskLevel;
    private List<String> riskReasons;
    private LocalDateTime analyzedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
