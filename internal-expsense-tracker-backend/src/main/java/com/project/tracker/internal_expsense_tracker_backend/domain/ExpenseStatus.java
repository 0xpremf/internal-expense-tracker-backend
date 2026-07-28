package com.project.tracker.internal_expsense_tracker_backend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExpenseStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DRAFT, SUBMITTED;

    @JsonCreator
    public static ExpenseStatus fromString(String s){
        if(s==null) return null;
        return ExpenseStatus.valueOf(s.toUpperCase());
    }

    @JsonValue
    public String toValue(){return name().toLowerCase();}


}
