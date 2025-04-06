package com.jbr.middletier.backup.manager.importing.step.process;

public class ImportProcessException extends Exception {
    public ImportProcessException(String message) {
        super(message);
    }

    public ImportProcessException(String message, Exception e) {
        super(message,e);
    }
}
