package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.CONFLICT)
public class BackupAlreadyExistsException extends Exception {
    public BackupAlreadyExistsException(String id) {
        super("Backup with id ("+id+") already exists.");
    }
}
