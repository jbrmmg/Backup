package com.jbr.middletier.backup.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActionConfirmDTO {
    private int id;
    private int fileId;
    private String fileName;
    private Long size;
    private LocalDateTime date;
    private String action;
    private Boolean confirmed;
    private Boolean parameterRequired;
    private String parameter;
    private String flags;
    private boolean isImage;
    private boolean isVideo;
}
