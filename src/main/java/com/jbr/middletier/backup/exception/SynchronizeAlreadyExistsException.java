package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.CONFLICT)
public class SynchronizeAlreadyExistsException extends Exception {
    public SynchronizeAlreadyExistsException(Integer id) {
        super("Synchronize with id ("+id+") already exists.");
    }
}
