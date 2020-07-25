package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class InvalidFileIdException extends Exception {
    public InvalidFileIdException(Integer id) {
        super("File with id ("+id.toString()+") not found.");
    }
}
