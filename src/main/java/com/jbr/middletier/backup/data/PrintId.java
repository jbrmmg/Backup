package com.jbr.middletier.backup.data;

import javax.persistence.Column;
import javax.validation.constraints.NotNull;

public class PrintId extends BaseComparable {
    @NotNull
    @Column(name="file_id")
    private Integer fileId;

    @NotNull
    @Column(name="size_id")
    private Integer sizeId;

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer fileId) {
        this.fileId = fileId;
    }

    public Integer getSizeId() {
        return sizeId;
    }

    public void setSizeId(Integer sizeId) {
        this.sizeId = sizeId;
    }

    @Override
    public String toString() {
        return this.fileId.toString() + "-" + this.sizeId.toString();
    }
}
