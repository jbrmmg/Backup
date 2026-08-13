package com.jbr.middletier.backup.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchResultDTO {
    private int id;
    private String name;
    private String fullFilename;
    private String path;
    private String locationName;
    private LocalDateTime date;
    private long size;
    private LocalDateTime expiry;
    private boolean isImage;
    private boolean isVideo;
    private String icon;
    private String md5;
    private Double latitude;
    private Double longitude;
}
