package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.CustomMetaData;
import lombok.Data;

@Data
public class CustomMetaDataDTO {
    private String originalFile;
    private String originalMd5;
    private Long originalSize;

    public CustomMetaDataDTO() {
    }

    public CustomMetaDataDTO(CustomMetaData customMetaData) {
        this.originalFile = customMetaData.getOriginalFile();
        this.originalMd5 = customMetaData.getOriginalMd5();
        this.originalSize = customMetaData.getOriginalSize();
    }
}
