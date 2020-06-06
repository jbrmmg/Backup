package com.jbr.middletier.backup.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class ActionNotFoundException extends RuntimeException {
    public ActionNotFoundException(int i) { super("Action " + i + " not found."); }
}
