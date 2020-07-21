package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.CONFLICT)
public class HardwareAlreadyExistsException extends Exception {
    public HardwareAlreadyExistsException(String id) {
        super("Hardware with id ("+id+") already exists.");
    }
}
