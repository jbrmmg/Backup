package com.jbr.middletier.backup.exception;

public class InvalidHardwareIdException extends Exception {
    public InvalidHardwareIdException(String id) {
        super("Hardware with id ("+id+") not found.");
    }
}
