package com.jbr.middletier.backup.dto;

import java.time.LocalDateTime;

@SuppressWarnings("unused")
public class FileInfoDTO {
    private String filename;
    private String type;
    private LocalDateTime date;
    private Long size;
    private String md5;
    private String parentType;
    private Integer parentId;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) { this.filename = filename; }

    public String getType() {
        return type;
    }

    public void setType(String type) { this.type = type; }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) { this.date = date;}

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) { this.size = size; }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) { this.md5 = md5; }

    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }
}
