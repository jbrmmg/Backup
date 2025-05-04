package com.jbr.middletier.backup.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ImportSourceDTO extends SourceDTO {
    private Integer destinationId;

    public ImportSourceDTO() {
        super();
    }
}
