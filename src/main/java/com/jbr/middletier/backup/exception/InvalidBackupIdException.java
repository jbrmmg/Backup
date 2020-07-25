package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND)
public class InvalidBackupIdException extends Exception{
    public InvalidBackupIdException(String id) {
        super("Backup with id ("+id+") not found.");
    }
}
