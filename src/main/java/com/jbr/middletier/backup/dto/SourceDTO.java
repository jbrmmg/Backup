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

    public SourceDTO() {
        setId(0);
        setPath("");
    }

    public SourceDTO(int id, String path) {
        setId(id);
        setPath(path);
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
}
