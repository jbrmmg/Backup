package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class InvalidSynchronizeIdException extends Exception {
    public InvalidSynchronizeIdException(Integer id) {
        super("Synchronize with id ("+id.toString()+") not found.");
    }
}
