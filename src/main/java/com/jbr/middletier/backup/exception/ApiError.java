package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@SuppressWarnings("unused")
class ApiError {
    private HttpStatus status;
    private LocalDateTime timestamp;
    private String message;
    private String debugMessage;

    private ApiError() {
        timestamp = LocalDateTime.now();
    }

    ApiError(HttpStatus status, String message, Throwable ex) {
        this();
        this.status = status;
        this.message = message;
        this.debugMessage = ex.getLocalizedMessage();
    }

    LocalDateTime getTimestamp() { return this.timestamp; }

    HttpStatus getStatus() { return this.status; }

    String getMessage() { return this.message; }

    String getDebugMessage() { return this.debugMessage; }
}
