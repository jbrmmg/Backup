package com.jbr.middletier.backup.dto;

public class SynchronizeDTO {
    private Integer id;
    private SourceDTO source;
    private SourceDTO destination;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public SourceDTO getSource() {
        return source;
    }

    public void setSource(SourceDTO source) {
        this.source = source;
    }

    public SourceDTO getDestination() {
        return destination;
    }

    public void setDestination(SourceDTO destination) {
        this.destination = destination;
    }
}
