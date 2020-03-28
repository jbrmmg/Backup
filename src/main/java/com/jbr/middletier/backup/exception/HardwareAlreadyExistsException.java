package com.jbr.middletier.backup.exception;

public class HardwareAlreadyExistsException extends Exception {
    public HardwareAlreadyExistsException(String id) {
        super("Hardware with id ("+id+") already exists.");
    }
}
