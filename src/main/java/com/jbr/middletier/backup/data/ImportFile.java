package com.jbr.middletier.backup.data;

import javax.persistence.*;

@Entity
@Table(name="import_file")
public class ImportFile extends FileInfo {
    @Column(name="status")
    private String status;

    public ImportFile() {
        super(FileSystemObjectType.FSO_IMPORT_FILE);
    }

    public void setStatus(ImportFileStatusType status) { this.status = status.getTypeName(); }

    public ImportFileStatusType getStatus() { return ImportFileStatusType.getFileStatusType(this.status); }
}
