package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.ImportSource;

public class ImportSourceDTO extends SourceDTO {
    private Integer destinationId;

    public ImportSourceDTO() {
        super();
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }
}
