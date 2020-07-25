package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.CONFLICT)
public class SourceAlreadyExistsException extends Exception {
    public SourceAlreadyExistsException(Integer id) {
        super("Source with id ("+id+") already exists.");
    }
}
