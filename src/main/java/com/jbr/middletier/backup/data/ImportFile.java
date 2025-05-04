package com.jbr.middletier.backup.data;

import javax.persistence.*;
import java.time.LocalDateTime;

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

    public String getImportName() {
        return importName;
    }

    public void setImportName(String importName) {
        this.importName = importName;
    }

    public LocalDateTime getImportDate() {
        return importDate;
    }

    public void setImportDate(LocalDateTime importDate) {
        this.importDate = importDate;
    }

    public Long getImportSize() {
        return importSize;
    }

    public void setImportSize(Long importSize) {
        this.importSize = importSize;
    }

    public String getImportMd5() {
        return importMd5;
    }

    public void setImportMd5(String importMd5) {
        this.importMd5 = importMd5;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Boolean getImage() {
        return image;
    }

    public void setImage(Boolean image) {
        this.image = image;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Boolean getVideo() {
        return video;
    }

    public void setVideo(Boolean video) {
        this.video = video;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }
}
