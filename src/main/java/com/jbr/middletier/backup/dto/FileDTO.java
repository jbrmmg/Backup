package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.MD5;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Optional;

@Data
public class FileDTO {
    private int id;
    private String name;
    private String fullFilename;
    private long size;
    private LocalDateTime date;
    @Getter
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
        if(fileInfo.getMd5().isPresent()) {
            this.md5 = fileInfo.getMd5().get().toString();
        } else {
            this.md5 = null;
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

    public void setMd5(MD5 md5) { this.md5 = md5 != null ? md5.toString() : null; }

    public Optional<MD5> getMd5Optional() { return this.md5 == null ? Optional.empty() : Optional.of(new MD5(this.md5)); }
}
