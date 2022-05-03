package com.jbr.middletier.backup.dto;

import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
public class SourceDTO {
    private Integer id;
    private String path;
    private LocationDTO location;
    private String status;
    private String filter;
    private String type;
    private Integer destinationId;
    private int directoryCount;
    private int fileCount;
    private long totalFileSize;
    private long largestFile;

    private void initialise() {
        this.directoryCount = 0;
        this.fileCount = 0;
        this.totalFileSize = 0;
        this.largestFile = 0;
    }

    public SourceDTO() {
        setId(null);
        setPath("");
        initialise();
    }

    public SourceDTO(int id, String path) {
        setId(id);
        setPath(path);
        initialise();
    }

    public Integer getId() {
        return id;
    }

    public void setId(@NotNull Integer id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(@NotNull String path) {
        this.path = path;
    }

    public LocationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationDTO location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }

    public int getDirectoryCount() {
        return directoryCount;
    }

    public int getFileCount() {
        return fileCount;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    public long getLargestFile() {
        return largestFile;
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
