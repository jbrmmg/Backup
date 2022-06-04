package com.jbr.middletier.backup.dto;

public class ImportSourceDTO extends SourceDTO {
    private Integer destinationId;

    public ImportSourceDTO() {
        super();
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
