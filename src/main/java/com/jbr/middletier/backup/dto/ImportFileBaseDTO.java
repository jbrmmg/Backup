package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.MD5;

import java.time.LocalDateTime;

public class ImportFileBaseDTO {
    private String filename;
    private LocalDateTime date;
    private Long size;
    private String md5;

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(MD5 md5) {
        this.md5 = md5.toString();
    }
}
