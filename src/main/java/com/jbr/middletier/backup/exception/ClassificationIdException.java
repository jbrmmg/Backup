package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.CONFLICT)
public class ClassificationIdException extends Exception {
    public ClassificationIdException() {
        super("Classification must not be specified on creation");
    }
}
