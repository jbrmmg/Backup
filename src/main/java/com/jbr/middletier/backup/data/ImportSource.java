package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name="import_source")
public class ImportSource extends Source {
    @JoinColumn(name="destination")
    @ManyToOne
    private Source destination;

    public ImportSource() {
        super(FileSystemObjectType.FSO_IMPORT_SOURCE);
        this.destination = null;
    }
}
