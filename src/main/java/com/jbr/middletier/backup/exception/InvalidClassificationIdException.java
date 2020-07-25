package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class InvalidClassificationIdException extends Exception {
    public InvalidClassificationIdException(Integer id) {
        super("Classification with id ("+id.toString()+") not found.");
    }
}
