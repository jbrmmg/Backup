package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileInfo;

import java.util.Date;

@SuppressWarnings("unused")
public class FileDTO {
    private int id;
    private String name;
    private String fullFilename;
    private long size;
    private Date date;
    private String md5;
    private boolean isImage;
    private boolean isVideo;
    private String icon;
    private String path;
    private String locationName;

    public FileDTO(FileInfo fileInfo, String fullFilename, String path, String location) {
        this.id = fileInfo.getIdAndType().getId();
        this.name = fileInfo.getName();
        this.date = fileInfo.getDate();
        this.size = fileInfo.getSize();
        this.md5 = fileInfo.getMD5().toString();
        this.isImage = fileInfo.getClassification().getIsImage();
        this.isVideo = fileInfo.getClassification().getIsVideo();
        this.icon = fileInfo.getClassification().getIcon();
        this.fullFilename = fullFilename;
        this.path = path;
        this.locationName = location;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullFilename() {
        return fullFilename;
    }

    public void setFullFilename(String fullFilename) {
        this.fullFilename = fullFilename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public boolean isImage() {
        return isImage;
    }

    public void setImage(boolean image) {
        isImage = image;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}