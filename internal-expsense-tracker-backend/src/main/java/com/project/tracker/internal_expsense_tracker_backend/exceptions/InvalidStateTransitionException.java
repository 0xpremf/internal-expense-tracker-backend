package com.project.tracker.internal_expsense_tracker_backend.exceptions;

public class InvalidStateTransitionException extends RuntimeException{
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
