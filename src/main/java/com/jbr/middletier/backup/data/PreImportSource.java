package com.jbr.middletier.backup.data;

import javax.persistence.*;

@Entity
@Table(name="pre_import_source")
public class PreImportSource extends Source {
    public PreImportSource() {
        super(FileSystemObjectType.FSO_PRE_IMPORT_SOURCE);
    }
}
