package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.util.ImageSize;
import com.jbr.middletier.backup.util.LatLong;

import java.util.ArrayList;
import java.util.List;

public class ImportFileDTO extends ImportFileBaseDTO {
    private Integer id;
    private String status;
    private LatLong location;
    private ImageSize imageSize;
    List<ImportFileBaseDTO> similarFileList;

    public ImportFileDTO() {
        similarFileList = new ArrayList<>();
    }

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
        this.similarFileList.add(file);
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
}
