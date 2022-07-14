package com.jbr.middletier.backup.data;

import javax.persistence.*;

@Entity
@Table(name="import_source")
public class ImportSource extends Source {
    @JoinColumn(name="destination")
    @ManyToOne
    private Source destination;

    public ImportSource() {
        super(FileSystemObjectType.FSO_IMPORT_SOURCE);
    }

    public ImportSource(String path) {
        super(FileSystemObjectType.FSO_IMPORT_SOURCE);
        setPath(path);
    }

    public Source getDestination() { return this.destination; }

    public void setDestination(Source destination) { this.destination = destination; }
}
