package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.PreImportSource;

public class PreImportSourceDTO extends SourceDTO {

    public PreImportSourceDTO() {
        super();
    }

    public PreImportSourceDTO(PreImportSource importSource) {
        this(new SourceDTO(importSource));
    }

    public PreImportSourceDTO(SourceDTO sourceDTO) {
        super(sourceDTO);
    }
}
