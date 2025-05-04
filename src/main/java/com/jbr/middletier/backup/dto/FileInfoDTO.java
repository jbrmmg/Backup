package com.jbr.middletier.backup.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileInfoDTO {
    private String filename;
    private String type;
    private LocalDateTime date;
    private Long size;
    private String md5;
    private String parentType;
    private Integer parentId;
    private LocalDateTime expiry;
}
