package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Setter
@Getter
@Embeddable
public class FileLabelId extends BaseComparable {
    @NotNull
    @Column(name="file_id")
    private Integer fileId;

    @NotNull
    @Column(name="label_id")
    private Integer labelId;

    @Override
    public String toString() {
        return this.fileId.toString() + "-" + this.labelId.toString();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
