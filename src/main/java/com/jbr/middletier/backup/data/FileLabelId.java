package com.jbr.middletier.backup.data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class FileLabelId extends BaseComparable {
    @NotNull
    @Column(name="file_id")
    private Integer fileId;

    @NotNull
    @Column(name="label_id")
    private Integer labelId;

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer fileId) {
        this.fileId = fileId;
    }

    public Integer getLabelId() {
        return labelId;
    }

    public void setLabelId(Integer labelId) {
        this.labelId = labelId;
    }

    @Override
    public String toString() {
        return this.fileId.toString() + "-" + this.labelId.toString();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
