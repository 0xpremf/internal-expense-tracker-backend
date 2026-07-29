package com.project.tracker.internal_expsense_tracker_backend.exceptions;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}