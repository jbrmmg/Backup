package com.jbr.middletier.backup.dto;

import lombok.Data;

@Data
public class SelectedPrintDTO {
    private int fileId;
    private String fileName;
    private int sizeId;
    private String sizeName;
    private Boolean border;
    private Boolean blackWhite;
}
