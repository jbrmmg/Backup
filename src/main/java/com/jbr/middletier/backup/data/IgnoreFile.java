package com.jbr.middletier.backup.data;

import javax.persistence.*;

@SuppressWarnings("unused")
@Entity
@Table(name="ignore_file")
public class IgnoreFile extends FileInfo {
    public IgnoreFile() {
        super(FileSystemObjectType.FSO_IGNORE_FILE);
    }
}
