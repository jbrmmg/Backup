package com.jbr.middletier.backup.dto;

import lombok.Data;

@Data
public class SynchronizeDTO {
    private Integer id;
    private SourceDTO source;
    private SourceDTO destination;

    public SynchronizeDTO() {
        setId(0);
    }
}
