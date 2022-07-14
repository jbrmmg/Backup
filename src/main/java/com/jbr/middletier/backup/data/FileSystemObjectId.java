package com.jbr.middletier.backup.data;

public class FileSystemObjectId {
    private final Integer id;
    private final FileSystemObjectType type;

    public FileSystemObjectId(Integer id, FileSystemObjectType type) {
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
        if(obj == null)
            return false;

        if(getClass() != obj.getClass())
            return false;

        final FileSystemObjectId rhsId = (FileSystemObjectId)obj;
        if(!this.type.getTypeName().equals(rhsId.type.getTypeName()))
            return false;

        return this.id.equals(rhsId.id);
    }

    public Integer getId() {
        return this.id;
    }

    public FileSystemObjectType getType() {
        return this.type;
    }
}
