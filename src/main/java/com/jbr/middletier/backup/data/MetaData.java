package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.manager.FileSystemImageData;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Setter
@Getter
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

    @Column(name="date")
    private LocalDateTime date;

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
        if(metaDataFromImage.getDateTime() != null){
            this.date = metaDataFromImage.getDateTime();
        }
    }

}
