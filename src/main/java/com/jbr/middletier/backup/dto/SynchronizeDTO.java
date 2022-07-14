package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.Synchronize;

import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
public class SynchronizeDTO {
    private Integer id;
    private SourceDTO source;
    private SourceDTO destination;

    public SynchronizeDTO() {
        setId(0);
    }

    public SynchronizeDTO(int id) {
        setId(id);
    }

    public SynchronizeDTO(Synchronize synchronize) {
        this.id = synchronize.getId();
        this.source = new SourceDTO(synchronize.getSource());
        this.destination = new SourceDTO(synchronize.getDestination());
    }

    public Integer getId() {
        return id;
    }

    public void setId(@NotNull Integer id) {
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
