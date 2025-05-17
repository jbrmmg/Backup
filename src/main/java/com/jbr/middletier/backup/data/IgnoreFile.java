package com.jbr.middletier.backup.data;

import jakarta.persistence.*;

@Entity
@Table(name="ignore_file")
public class IgnoreFile extends FileInfo {
    public IgnoreFile() {
        super(FileSystemObjectType.FSO_IGNORE_FILE);
    }
}
