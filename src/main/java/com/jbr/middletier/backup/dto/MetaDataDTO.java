package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.MetaData;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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
}
