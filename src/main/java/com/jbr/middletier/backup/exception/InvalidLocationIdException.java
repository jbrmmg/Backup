package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class InvalidLocationIdException extends Exception {
    public InvalidLocationIdException(Integer id) {
        super("Location with id ("+id+") not found.");
    }
}
