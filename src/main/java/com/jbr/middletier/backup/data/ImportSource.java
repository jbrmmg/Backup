package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.ImportSourceDTO;
import com.jbr.middletier.backup.dto.SourceDTO;

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

    public ImportSource(ImportSourceDTO source) {
        this();
        update(source);
        // TODO - fix this
//        this.destinationId = source.getDestinationId();
    }

    public ImportSourceDTO getImportSourceDTO() {
        ImportSourceDTO result = new ImportSourceDTO(getSourceDTO());
        // TODO - fix this
//        result.setDestinationId(getDestinationId());

        return result;
    }

    public Source getDestination() { return this.destination; }

    public void setDestination(Source destination) { this.destination = destination; }
}
