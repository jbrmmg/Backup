package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.ImportFileStatusType;
import java.util.ArrayList;
import java.util.List;

public class ImportFileDTO extends ImportFileBaseDTO {
    private Integer id;
    private String status;
    List<ImportFileBaseDTO> similarFileList;

    public ImportFileDTO() {
        similarFileList = new ArrayList<>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(ImportFileStatusType status) {
        this.status = status.getTypeName();
    }

    public void addSimilarFile(ImportFileBaseDTO file) {
        this.similarFileList.add(file);
    }

    public List<ImportFileBaseDTO> getSimilarFiles() { return this.similarFileList; }
}
