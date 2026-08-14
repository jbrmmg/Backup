package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.manager.FileSystemCustomData;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Setter
@Getter
@Entity
@Table(name="custom_meta_data")
public class CustomMetaData {
    @Id
    @Column(name="id")
    private Integer id;

    @Column(name="original_file")
    private String originalFile;

    @Column(name="original_md5")
    private String originalMd5;

    @Column(name="original_size")
    private Long originalSize;

    public CustomMetaData() {
    }

    public CustomMetaData(Integer id, FileSystemCustomData customData) {
        this.id = id;
        this.originalFile = customData.getOriginalFile();
        this.originalMd5 = customData.getOriginalMd5() != null ? customData.getOriginalMd5().toUpperCase() : null;
        this.originalSize = customData.getOriginalSize();
    }
}
