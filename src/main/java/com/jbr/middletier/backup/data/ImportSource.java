package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.ImportSourceDTO;
import com.jbr.middletier.backup.dto.SourceDTO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="imp_source")
public class ImportSource extends Source {
    @Column(name="destination")
    private Integer destinationId;

    public ImportSource() {
        super(true, "IMP");
    }

    public ImportSource(String path) {
        super(true, "IMP");
        setPath(path);
    }

    public ImportSource(ImportSourceDTO source) {
        this();
        update(source);
        this.destinationId = source.getDestinationId();
    }

    public ImportSourceDTO getImportSourceDTO() {
        ImportSourceDTO result = new ImportSourceDTO(getSourceDTO());
        result.setDestinationId(getDestinationId());

        return result;
    }

    public Integer getDestinationId() { return this.destinationId; }

    public void setDestinationId(Integer id) { this.destinationId = id; }
}
