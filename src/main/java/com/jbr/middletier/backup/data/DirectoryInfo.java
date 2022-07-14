package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
@Entity
@Table(name="directory")
public class DirectoryInfo extends FileSystemObject {
    @Column(name="removed")
    @NotNull
    private Boolean removed;

    public DirectoryInfo() {
        super(FileSystemObjectType.FSO_DIRECTORY);
    }

    public Boolean getRemoved() { return this.removed; }

    public void clearRemoved() { this.removed = false; }

    public void setRemoved() { this.removed = true; }

    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "DirectoryInfo: " + name;
    }
}
