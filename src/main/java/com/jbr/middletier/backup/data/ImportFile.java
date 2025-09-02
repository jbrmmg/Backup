package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Setter
@Getter
@Entity
@Table(name="import_file")
public class ImportFile extends FileInfo {
    @Column(name="import_name")
    private String importName;

    @Column(name="import_date")
    private LocalDateTime importDate;

    @Column(name="import_size")
    private Long importSize;

    @Column(name="import_md5")
    private String importMd5;

    @Column(name="destination")
    private String destination;

    @Column(name="image")
    private Boolean image;

    @Column(name="image_height")
    private Integer imageHeight;

    @Column(name="image_width")
    private Integer imageWidth;

    @Column(name="lat")
    private Double latitude;

    @Column(name="\"long\"")
    private Double longitude;

    @Column(name="video")
    private Boolean video;

    @Column(name="duration")
    private Double duration;

    public ImportFile() {
        super(FileSystemObjectType.FSO_IMPORT_FILE);
    }

    public void setImportMd5(MD5 md5) { this.importMd5 = md5 != null ? md5.toString() : null; }

    public Optional<MD5> getImportMd5() { return this.importMd5 == null ? Optional.empty() : Optional.of(new MD5(this.importMd5)); }
}
