package com.project.tracker.internal_expsense_tracker_backend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ExpenseCategory {
    TRAVEL,
    MEALS,
    EQUIPMENT,
    SOFTWARE,
    OTHER;

    @JsonCreator
    public static ExpenseCategory fromString(String value) {
        if(value==null){return null;}
        return ExpenseCategory.valueOf(value.toUpperCase());
    }
    @JsonValue
    public String toString(){return name().toLowerCase();}
}
