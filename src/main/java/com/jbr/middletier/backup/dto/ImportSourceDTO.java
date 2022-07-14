package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.ImportSource;

public class ImportSourceDTO extends SourceDTO {
    private Integer destinationId;

    public ImportSourceDTO() {
        super();
    }

    public ImportSourceDTO(ImportSource importSource) {
        this(new SourceDTO(importSource));
        this.destinationId = importSource.getDestination().getIdAndType().getId();
    }

    public ImportSourceDTO(SourceDTO sourceDTO) {
        super(sourceDTO);
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }
}
