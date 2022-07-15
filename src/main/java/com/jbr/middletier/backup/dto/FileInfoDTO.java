package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.MD5;

import java.time.LocalDateTime;

//import java.util.Date;

@SuppressWarnings("unused")
public class FileInfoDTO {
    private final String filename;
    private final FileSystemObjectType type;
    private final LocalDateTime date;
    private final Long size;
    private final MD5 md5;
    private final FileSystemObjectType parentType;

    public FileInfoDTO(FileInfo fileInfo) {
        this.filename = fileInfo.getName();
        this.type = fileInfo.getIdAndType().getType();
        this.date = fileInfo.getDate();
        this.size = fileInfo.getSize();
        this.md5 = fileInfo.getMD5();
        this.parentType = fileInfo.getParentId() == null ?  null : fileInfo.getParentId().getType();
    }

    public FileInfoDTO(FileSystemObject fso) {
        this((FileInfo)fso);
    }

    public String getFilename() {
        return filename;
    }

    public FileSystemObjectType getType() {
        return type;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Long getSize() {
        return size;
    }

    public MD5 getMd5() {
        return md5;
    }

    public FileSystemObjectType getParentType() {
        return parentType;
    }
}
