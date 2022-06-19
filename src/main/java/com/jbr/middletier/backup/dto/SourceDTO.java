package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.data.SourceStatusType;

@SuppressWarnings("unused")
public class SourceDTO {
    private Integer id;
    private String path;
    private LocationDTO location;
    private SourceStatusType status;
    private String filter;
    private int directoryCount;
    private int fileCount;
    private long totalFileSize;
    private long largestFile;

    public SourceDTO() {
        setId(null);
        setPath("");
        this.directoryCount = 0;
        this.fileCount = 0;
        this.totalFileSize = 0;
        this.largestFile = 0;
    }

    public SourceDTO(Source source) {
        this();
        setId(source.getIdAndType().getId());
        setPath(source.getPath());
        setLocation(new LocationDTO(source.getLocation()));
        setStatus(source.getStatus());
        setFilter(source.getFilter());
    }

    public SourceDTO(int id, String path) {
        this();
        setId(id);
        setPath(path);
    }

    protected SourceDTO(SourceDTO sourceDTO) {
        this();
        setId(sourceDTO.getId());
        setPath(sourceDTO.getPath());
        setLocation(sourceDTO.getLocation());
        setStatus(sourceDTO.getStatus());
        setFilter(sourceDTO.getFilter());
        this.directoryCount = 0;
        this.fileCount = 0;
        this.totalFileSize = 0;
        this.largestFile = 0;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationDTO location) {
        this.location = location;
    }

    public SourceStatusType getStatus() {
        return status;
    }

    public void setStatus(SourceStatusType status) {
        this.status = status;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
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
