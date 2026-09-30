package com.example.demo.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response wrapper for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseWrapper<T> {
    
    private boolean success;
    private String message;
    private T data;
    private String error;
    
    /**
     * Create successful response with data
     */
    public static <T> ResponseWrapper<T> success(T data, String message) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
    
    /**
     * Create successful response with data (default message)
     */
    public static <T> ResponseWrapper<T> success(T data) {
        return success(data, "Success");
    }
    
    /**
     * Create error response
     */
    public static <T> ResponseWrapper<T> error(String error) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }
    
    /**
     * Create error response with message
     */
    public static <T> ResponseWrapper<T> error(String error, String message) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setSuccess(false);
        response.setError(error);
        response.setMessage(message);
        return response;
    }
}
