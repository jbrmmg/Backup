package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.MetaData;
import java.time.LocalDateTime;

public class MetaDataDTO {
    private Boolean image;
    private Integer imageHeight;
    private Integer imageWidth;
    private Double latitude;
    private Double longitude;
    private Boolean video;
    private Double duration;
    private LocalDateTime date;

    public MetaDataDTO() {
    }

    public MetaDataDTO(MetaData metaData) {
        this.image = metaData.getImage();
        this.video = metaData.getVideo();
        this.imageHeight = metaData.getImageHeight();
        this.imageWidth = metaData.getImageWidth();
        this.latitude = metaData.getLatitude();
        this.longitude = metaData.getLongitude();
        this.duration = metaData.getDuration();
        this.date = metaData.getDate();
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

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
