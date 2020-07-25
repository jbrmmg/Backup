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

    @ExceptionHandler(InvalidFileIdException.class)
    protected ResponseEntity<Object> handleInvalidFileId(InvalidFileIdException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Invalid file id", ex));
    }

    @ExceptionHandler({InvalidMediaTypeException.class})
    public ResponseEntity<Object> handleInvalidMidiaTypeException(InvalidMediaTypeException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.BAD_REQUEST,"Invalid file type for request.",ex));
    }

    @ExceptionHandler({ImportRequestException.class})
    public ResponseEntity<Object> handleAll(ImportRequestException ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Cannot find source or import directory",ex));
    }

    @ExceptionHandler({InvalidSynchronizeIdException.class})
    public ResponseEntity<Object> handleAll(InvalidSynchronizeIdException ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Invalid Synchronize ID",ex));
    }

    @ExceptionHandler({SynchronizeAlreadyExistsException.class})
    public ResponseEntity<Object> handleAll(SynchronizeAlreadyExistsException ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Synchronize already exists",ex));
    }

    @ExceptionHandler({InvalidSourceIdException.class})
    public ResponseEntity<Object> handleAll(InvalidSourceIdException ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Source Synchronize ID",ex));
    }

    @ExceptionHandler({SourceAlreadyExistsException.class})
    public ResponseEntity<Object> handleAll(SourceAlreadyExistsException ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Source already exists",ex));
    }

    @ExceptionHandler({InvalidClassificationIdException.class})
    public ResponseEntity<Object> handleAll(InvalidClassificationIdException ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Classification not found",ex));
    }

    @ExceptionHandler({ClassificationIdException.class})
    public ResponseEntity<Object> handleAll(ClassificationIdException ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Classification id must be null on create",ex));
    }

    @ExceptionHandler({InvalidLocationIdException.class})
    public ResponseEntity<Object> handleAll(InvalidLocationIdException ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Location not found",ex));
    }

    @ExceptionHandler({LocationAlreadyExistsException.class})
    public ResponseEntity<Object> handleAll(LocationAlreadyExistsException ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Location already exists",ex));
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.BAD_REQUEST,"Unexpected Exception",ex));
    }

    private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError,apiError.getStatus());
    }
}
