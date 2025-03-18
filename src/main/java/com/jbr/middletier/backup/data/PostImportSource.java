package com.jbr.middletier.backup.data;

import javax.persistence.*;

@Entity
@Table(name="post_import_source")
public class PostImportSource extends Source {
    public PostImportSource() {
        super(FileSystemObjectType.FSO_POST_IMPORT_SOURCE);
    }
}
