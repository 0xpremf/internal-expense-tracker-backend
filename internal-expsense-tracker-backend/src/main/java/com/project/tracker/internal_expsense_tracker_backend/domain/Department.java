package com.project.tracker.internal_expsense_tracker_backend.domain;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "departments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private String name;


    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "monthly_budget", precision = 12, scale = 2)
    private BigDecimal monthlyBudget;



}
