package com.cme.pricingValidation.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonParseError(HttpMessageNotReadableException ex,
                                                  HttpServletRequest request) {

        log.warn("JSON parse error on {}: {}", request.getRequestURI(), ex.getMessage());

        Throwable root = ex.getMostSpecificCause();
        String message = "Malformed JSON request";

        if (root != null && root.getMessage() != null) {
            if (root.getMessage().contains("java.math.BigDecimal")) {
                message = "Invalid numeric format in request body (e.g. price must be a valid number)";
            } else {
                message = root.getMessage();
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "error", "Bad Request",
                        "message", message,
                        "path", request.getRequestURI()
                )
        );
    }
}
