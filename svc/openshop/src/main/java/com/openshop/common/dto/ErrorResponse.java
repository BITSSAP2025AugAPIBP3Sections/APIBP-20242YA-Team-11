package com.openshop.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response format for all API errors
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String message;
    private String error;
    private int status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String path;
    private Map<String, String> validationErrors;
    private String errorCode;
    
    public static ErrorResponse of(String message, int status) {
        return ErrorResponse.builder()
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse of(String message, int status, String errorCode) {
        return ErrorResponse.builder()
                .message(message)
                .status(status)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse withValidationErrors(Map<String, String> validationErrors, int status) {
        return ErrorResponse.builder()
                .message("Validation failed")
                .status(status)
                .validationErrors(validationErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
