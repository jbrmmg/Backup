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

    @Override
    public int hashCode() {
        return this.type.hashCode() * this.id;
    }

    @Override
    public boolean equals(final Object obj) {
        if(this == obj)
            return true;

        if(obj == null)
            return false;

        if(getClass() != obj.getClass())
            return false;

        final FileSystemObjectId id = (FileSystemObjectId)obj;
        if(!this.type.getTypeName().equals(id.type.getTypeName()))
            return false;

        return this.id == id.id;
    }

    public int getId() {
        return this.id;
    }

    public FileSystemObjectType getType() {
        return this.type;
    }
}
