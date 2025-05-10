package com.jbr.middletier.backup.dto;

import lombok.Data;
import jakarta.validation.constraints.Pattern;

@Data
public class DestinationUpdateDTO {
    @Pattern(regexp="^[\\w\\-. ]+$",message="Filename cannot contain special characters.")
    private String filename;
    @Pattern(regexp="^[0-9a-zA-Z]+$",message="Destination can only be letters and numbers.")
    private String destination;

    public DestinationUpdateDTO() {
        this.filename = null;
        this.destination = null;
    }
}
