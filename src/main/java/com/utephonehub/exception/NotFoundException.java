package com.utephonehub.exception;

/**
 * Not Found Exception
 * Exception cho các trường hợp không tìm thấy resource
 */
public class NotFoundException extends Exception {
    
    public NotFoundException(String message) {
        super(message);
    }
    
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
