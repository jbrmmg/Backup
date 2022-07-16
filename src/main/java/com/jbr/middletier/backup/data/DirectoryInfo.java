package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
@Entity
@Table(name="directory")
public class DirectoryInfo extends FileSystemObject {
    public DirectoryInfo() {
        super(FileSystemObjectType.FSO_DIRECTORY);
    }

    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "DirectoryInfo: " + name;
    }
}
