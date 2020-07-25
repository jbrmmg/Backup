package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.CONFLICT)
public class LocationAlreadyExistsException extends Exception {
    public LocationAlreadyExistsException(Integer id) {
        super("Location with id ("+id+") already exists.");
    }
}
