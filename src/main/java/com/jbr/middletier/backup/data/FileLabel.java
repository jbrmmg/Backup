package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Setter
@Getter
@Entity
@Table(name="file_label")
public class FileLabel {
    @EmbeddedId
    private FileLabelId id;
}
