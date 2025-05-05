package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.validation.constraints.NotNull;

@Setter
@Getter
public class PrintId extends BaseComparable {
    @NotNull
    @Column(name="file_id")
    private Integer fileId;

    @NotNull
    @Column(name="size_id")
    private Integer sizeId;

    @Override
    public String toString() {
        return this.fileId.toString() + "-" + this.sizeId.toString();
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
