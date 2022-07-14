package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.ImportFileStatusType;

public class ImportFileDTO {
    private final String filename;
    private final ImportFileStatusType status;

    public ImportFileDTO(ImportFile file) {
        this.filename = file.getName();
        this.status = file.getStatus();
    }

    public String getFilename() {
        return filename;
    }

    public ImportFileStatusType getStatus() {
        return status;
    }
}
