package com.jbr.middletier.backup.exception;

public class BackupAlreadyExistsException extends Exception {
    public BackupAlreadyExistsException(String id) {
        super("Backup with id ("+id+") already exists.");
    }
}
