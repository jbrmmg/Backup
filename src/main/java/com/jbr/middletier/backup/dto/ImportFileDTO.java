package com.jbr.middletier.backup.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.util.ImageSize;
import com.jbr.middletier.backup.util.LatLong;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImportFileDTO extends ImportFileBaseDTO {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFileDTO.class);

    @Setter
    private volatile Integer id;
    @Getter
    private volatile String status;
    private LatLong location;
    private ImageSize imageSize;
    @Getter
    private volatile boolean image;
    @Getter
    private volatile boolean video;
    private final List<ImportFileBaseDTO> similarFileList;

    public ImportFileDTO() {
        this.image = false;
        this.video = false;
        this.similarFileList = new ArrayList<>();
    }

    @JsonIgnore
    public Integer getId() {
        return id;
    }

    public void setStatus(ImportFileStatusType status) {
        LOG.info("File {} status changed from {} to {}", getFilename(), this.status, status);
        this.status = status.getTypeName();
    }

    public void addSimilarFile(ImportFileBaseDTO file) {
        // Log that this file is being added.
        LOG.info("Similar File for {}, {} - {} - {} - {}", this.getFilename(), file.getFilename(), file.getSize(), file.getMd5(), file.getDate());

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

    public synchronized LatLong getLocation() {
        return location;
    }

    public synchronized void setLocation(LatLong location) {
        this.location = location;
    }

    public synchronized ImageSize getImageSize() {
        return imageSize;
    }

    public synchronized void setImageSize(ImageSize imageSize) {
        this.imageSize = imageSize;
    }

    public void setImage(Boolean image) {
        if(image == null){
            this.image = false;
            return;
        }

        this.image = image;
    }

    public void setVideo(Boolean video) {
        if(video == null){
            this.video = false;
            return;
        }

        this.video = video;
    }
}
