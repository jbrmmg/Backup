package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.util.ImageSize;
import com.jbr.middletier.backup.util.LatLong;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImportFileDTO extends ImportFileBaseDTO {
    private Integer id;
    private String status;
    private LatLong location;
    private ImageSize imageSize;
    private boolean image;
    private boolean video;
    List<ImportFileBaseDTO> similarFileList;

    public ImportFileDTO() {
        this.image = false;
        this.video = false;
        this.similarFileList = new ArrayList<>();
    }

    @JsonIgnore
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(ImportFileStatusType status) {
        this.status = status.getTypeName();
    }

    public void addSimilarFile(ImportFileBaseDTO file) {
        // Only add if the file is not already in the list (by name)
        AtomicBoolean alreadyExists = new AtomicBoolean(false);
        this.similarFileList.forEach(f -> {
            if(f.getFilename().equalsIgnoreCase(file.getFilename())) {
                alreadyExists.set(true);
            }
        });

        if(!alreadyExists.get()){
            this.similarFileList.add(file);
        }
    }

    public List<ImportFileBaseDTO> getSimilarFiles() { return this.similarFileList; }

    public LatLong getLocation() {
        return location;
    }

    public void setLocation(LatLong location) {
        this.location = location;
    }

    public ImageSize getImageSize() {
        return imageSize;
    }

    public void setImageSize(ImageSize imageSize) {
        this.imageSize = imageSize;
    }

    public boolean isImage() {
        return image;
    }

    public void setImage(Boolean image) {
        if(image == null){
            this.image = false;
            return;
        }

        this.image = image;
    }

    public boolean isVideo() {
        return video;
    }

    public void setVideo(Boolean video) {
        if(video == null){
            this.video = false;
            return;
        }

        this.video = video;
    }
}
