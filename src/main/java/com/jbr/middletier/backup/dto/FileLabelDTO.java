package com.jbr.middletier.backup.dto;

import java.util.ArrayList;
import java.util.List;

public class FileLabelDTO {
    private Integer fileId;
    private final List<Integer> labels;

    public FileLabelDTO() {
        this.labels = new ArrayList<>();
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer fileId) {
        this.fileId = fileId;
    }

    public List<Integer> getLabels() {
        return labels;
    }
}
