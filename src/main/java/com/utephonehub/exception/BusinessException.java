package com.utephonehub.exception;

/**
 * Business Exception
 * Exception cho các lỗi business logic
 */
public class BusinessException extends Exception {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
