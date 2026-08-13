package com.jbr.middletier.backup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class FileMetaDataMissingException extends Exception {
    public FileMetaDataMissingException(Integer id) {
        super("File with id (" + id + ") does not have metadata.");
    }
}
