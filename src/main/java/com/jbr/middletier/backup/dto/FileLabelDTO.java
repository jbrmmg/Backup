package com.jbr.middletier.backup.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FileLabelDTO {
    private Integer fileId;
    private final List<Integer> labels;

    public FileLabelDTO() {
        this.labels = new ArrayList<>();
    }
}
