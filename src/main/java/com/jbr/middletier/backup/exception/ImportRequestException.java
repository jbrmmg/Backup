package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class ImportRequestException extends Exception {
    public ImportRequestException(String message) {
        super(message);
    }
}
