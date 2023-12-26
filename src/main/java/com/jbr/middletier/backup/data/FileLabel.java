package com.jbr.middletier.backup.data;

import javax.persistence.*;

@Entity
@Table(name="file_label")
public class FileLabel {
    @EmbeddedId
    private FileLabelId id;

    public FileLabelId getId() {
        return id;
    }

    public void setId(FileLabelId id) {
        this.id = id;
    }
}
