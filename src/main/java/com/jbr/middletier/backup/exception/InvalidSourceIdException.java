package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class InvalidSourceIdException extends Exception {
    public InvalidSourceIdException(Integer id) {
        super("Source with id ("+id.toString()+") not found.");
    }
}
