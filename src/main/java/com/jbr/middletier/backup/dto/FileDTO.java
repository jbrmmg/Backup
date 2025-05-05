package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileInfo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileDTO {
    private int id;
    private String name;
    private String fullFilename;
    private long size;
    private LocalDateTime date;
    private String md5;
    private boolean isImage;
    private boolean isVideo;
    private String icon;
    private String path;
    private String locationName;
    private LocalDateTime expiry;

    public FileDTO(FileInfo fileInfo, String fullFilename, String path, String location) {
        this.id = fileInfo.getIdAndType().getId();
        this.name = fileInfo.getName();
        this.date = fileInfo.getDate();
        this.size = fileInfo.getSize();
        if(fileInfo.getMD5() !=null) {
            this.md5 = fileInfo.getMD5().toString();
        } else {
            this.md5 = "";
        }
        if(fileInfo.getClassification() !=null) {
            this.isImage = fileInfo.getClassification().getIsImage();
            this.isVideo = fileInfo.getClassification().getIsVideo();
            this.icon = fileInfo.getClassification().getIcon();
        } else {
            this.isImage = false;
            this.isVideo = false;
            this.icon = "fa-file-o";
        }
        this.fullFilename = fullFilename;
        this.path = path;
        this.locationName = location;
        this.expiry = fileInfo.getExpiry();
    }
}
