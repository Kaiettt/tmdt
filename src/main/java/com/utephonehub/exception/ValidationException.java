package com.utephonehub.exception;

/**
 * Validation Exception
 * Exception cho các lỗi validation
 */
public class ValidationException extends Exception {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
