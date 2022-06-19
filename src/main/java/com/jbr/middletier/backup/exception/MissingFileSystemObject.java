package com.jbr.middletier.backup.exception;

import com.jbr.middletier.backup.data.FileSystemObjectId;

public class MissingFileSystemObject extends Exception {
    public MissingFileSystemObject(String additional, FileSystemObjectId fileSystemObjectId) {
        super("Cannot find file system object with id " + fileSystemObjectId + " (" + additional + ")");
    }
}
