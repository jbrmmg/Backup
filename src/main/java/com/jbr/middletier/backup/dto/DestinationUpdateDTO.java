package com.jbr.middletier.backup.dto;

public class DestinationUpdateDTO {
    private String filename;
    private String destination;

    public DestinationUpdateDTO() {
        this.filename = null;
        this.destination = null;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
