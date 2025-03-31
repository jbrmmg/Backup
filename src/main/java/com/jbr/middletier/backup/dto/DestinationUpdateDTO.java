package com.jbr.middletier.backup.dto;

import javax.validation.constraints.Pattern;

public class DestinationUpdateDTO {
    @Pattern(regexp="^[\\w\\-. ]+$",message="Filename cannot contain special characters.")
    private String filename;
    @Pattern(regexp="^[0-9a-zA-Z]+$",message="Destination can only be letters and numbers.")
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
