package com.project.tracker.internal_expsense_tracker_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalResultResponse {
    private ExpenseResponse expense;
    private String warning;
}
