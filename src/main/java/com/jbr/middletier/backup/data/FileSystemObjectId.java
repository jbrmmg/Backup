package com.jbr.middletier.backup.data;

public class FileSystemObjectId {
    private final int id;
    private final FileSystemObjectType type;

    public FileSystemObjectId(int id, FileSystemObjectType type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type + ">" + this.id;
    }

    public int getId() {
        return this.id;
    }

    public FileSystemObjectType getType() {
        return this.type;
    }
}
