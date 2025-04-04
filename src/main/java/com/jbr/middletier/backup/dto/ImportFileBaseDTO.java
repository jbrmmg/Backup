package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.MD5;
import com.jbr.middletier.backup.manager.importing.ImportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class ImportFileBaseDTO {
    private static final Logger LOG = LoggerFactory.getLogger(ImportManager.class);

    private volatile String filename;
    private volatile LocalDateTime date;
    private volatile Long size;
    private volatile String md5;
    private volatile FileSystemObjectType type;

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

    public FileSystemObjectType getType() {
        return type;
    }

    public void setType(FileSystemObjectType type) {
        this.type = type;
    }
}
