package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.manager.FileSystemImageData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="meta_data")
public class MetaData {
    @Id
    @Column(name="id")
    private Integer id;

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

    public MetaData() {
    }

    public MetaData(Integer id, FileSystemImageData metaDataFromImage) {
        this.id = id;
        this.image = metaDataFromImage.isImage();
        this.video = metaDataFromImage.isVideo();
        if(metaDataFromImage.getImageSize() != null){
            this.imageHeight = metaDataFromImage.getImageSize().height();
            this.imageWidth = metaDataFromImage.getImageSize().width();
        }
        if(metaDataFromImage.getLatLong() != null){
            this.latitude = metaDataFromImage.getLatLong().getLatitude();
            this.longitude = metaDataFromImage.getLatLong().getLongitude();
        }
        if(metaDataFromImage.getDuration() != null){
            this.duration = metaDataFromImage.getDuration();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
