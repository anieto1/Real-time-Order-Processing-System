package com.pm.orderservice.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ValidationErrorResponse extends ErrorResponse {
    private Map<String, String> validationErrors;
    private java.time.LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
