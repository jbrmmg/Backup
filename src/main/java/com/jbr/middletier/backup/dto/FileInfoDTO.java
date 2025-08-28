package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.MD5;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
public class FileInfoDTO {
    private String filename;
    private String type;
    private LocalDateTime date;
    private Long size;
    @Getter
    private String md5;
    private String parentType;
    private Integer parentId;
    private LocalDateTime expiry;

    public void setMd5(MD5 md5) { this.md5 = md5 != null ? md5.toString() : null; }
}
