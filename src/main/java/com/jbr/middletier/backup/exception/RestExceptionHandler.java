package com.jbr.middletier.backup.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(BackupAlreadyExistsException.class)
    protected ResponseEntity<Object> handleBackupAlreadyExist(BackupAlreadyExistsException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Backup already exists", ex));
    }

    @ExceptionHandler(ActionNotFoundException.class)
    protected ResponseEntity<Object> handleActionNotFound(ActionNotFoundException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Action Not Found", ex));
    }

    @ExceptionHandler(HardwareAlreadyExistsException.class)
    protected ResponseEntity<Object> handleHardwareAlreadyExist(HardwareAlreadyExistsException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Hardware already exists", ex));
    }

    @ExceptionHandler(InvalidBackupIdException.class)
    protected ResponseEntity<Object> handleInvalidBackupId(InvalidBackupIdException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Invalid backup id", ex));
    }

    @ExceptionHandler(InvalidHardwareIdException.class)
    protected ResponseEntity<Object> handleInvalidHardwareId(InvalidHardwareIdException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Invalid hardware id", ex));
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.BAD_REQUEST,"Unexpected Exception",ex));
    }

    private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError,apiError.getStatus());
    }
}
