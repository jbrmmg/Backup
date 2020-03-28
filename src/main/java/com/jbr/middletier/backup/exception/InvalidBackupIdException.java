package com.jbr.middletier.backup.exception;

public class InvalidBackupIdException extends Exception{
    public InvalidBackupIdException(String id) {
        super("Backup with id ("+id+") not found.");
    }
}
