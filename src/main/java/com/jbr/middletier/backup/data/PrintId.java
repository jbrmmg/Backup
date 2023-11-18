package com.jbr.middletier.backup.data;

import javax.persistence.Column;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class PrintId implements Serializable {
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
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof FileLabelId fileLabelId)) {
            return false;
        }

        return this.toString().equalsIgnoreCase(fileLabelId.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return this.fileId.toString() + "-" + this.sizeId.toString();
    }
}
