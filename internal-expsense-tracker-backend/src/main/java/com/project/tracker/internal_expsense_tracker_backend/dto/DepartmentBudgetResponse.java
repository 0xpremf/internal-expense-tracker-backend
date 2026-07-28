package com.project.tracker.internal_expsense_tracker_backend.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentBudgetResponse {
    private Long departmentId;
    private String departmentName;
    private String month; // e.g. "2026-07"
    private BigDecimal budget;
    private BigDecimal spent;
    private BigDecimal remaining;
    private boolean overBudget;

}
