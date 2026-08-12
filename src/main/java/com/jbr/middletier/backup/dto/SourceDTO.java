package com.jbr.middletier.backup.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SourceDTO {
    private Integer id;
    private String path;
    private LocationDTO location;
    private String status;
    private String filter;
    private String mountCheck;
    private String type;
    private int directoryCount;
    private int fileCount;
    private long totalFileSize;
    private long largestFile;
    private Boolean gatherMetaData;
    private Boolean primary;
    private String group;
    private LocalDateTime gatherStart;
    private LocalDateTime gatherFinished;
    private LocalDateTime syncStartTime;
    private LocalDateTime syncEndTime;
    private Integer syncFilesCopied;
    private Integer syncDirectoriesCopied;
    private Integer syncFilesDeleted;
    private Integer syncDirectoriesDeleted;
    private Integer syncSourcesRemoved;
    private Integer syncDatesUpdated;
    private Integer syncFilesWarned;

    public SourceDTO() {
        setId(null);
        setPath("");
        this.directoryCount = 0;
        this.fileCount = 0;
        this.totalFileSize = 0;
        this.largestFile = 0;
        this.mountCheck = null;
        this.primary = false;
    }
    public void incrementDirectoryCount() {
        this.directoryCount++;
    }

    public void incrementFileCount() {
        this.fileCount++;
    }

    public void increaseFileSize(long fileSize) {
        this.totalFileSize += fileSize;

        if(fileSize > this.largestFile) {
            this.largestFile = fileSize;
        }
    }
}
