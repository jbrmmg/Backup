package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class InvalidHardwareIdException extends Exception {
    public InvalidHardwareIdException(String id) {
        super("Hardware with id ("+id+") not found.");
    }
}
