package com.task_flow.backend.exception;

public class RetryableStepException extends RuntimeException {
    
    public RetryableStepException(String message) {
        super(message);
    }
    
    public RetryableStepException(String message, Throwable cause) {
        super(message, cause);
    }
}
