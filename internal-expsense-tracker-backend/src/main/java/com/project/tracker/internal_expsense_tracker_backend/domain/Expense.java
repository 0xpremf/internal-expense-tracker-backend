package com.project.tracker.internal_expsense_tracker_backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency; // ISO 4217, e.g., USD, EUR, GBP

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "receipt_url")
    private String receiptUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Fraud & Risk Analysis Fields
    @Column(name = "risk_score")
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_risk_reasons", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "reason")
    @Builder.Default
    private List<String> riskReasons = new ArrayList<>();

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    // Optimistic locking version field for concurrency safety
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

