package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.BAD_REQUEST)
public class InvalidMediaTypeException extends Exception  {
    public InvalidMediaTypeException(String type) {
        super("File is not of type " + type);
    }
}
