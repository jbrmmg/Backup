package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.ImportSourceDTO;
import com.jbr.middletier.backup.exception.InvalidSourceIdException;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;

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

    public ImportSource(ImportSourceDTO source, AssociatedFileDataManager associatedFileDataManager) throws InvalidSourceIdException {
        super(FileSystemObjectType.FSO_IMPORT_SOURCE);
        update(source);
        setDestination(associatedFileDataManager.internalFindSourceById(source.getId()));
    }

    public Source getDestination() { return this.destination; }

    public void setDestination(Source destination) { this.destination = destination; }
}
