package com.jbr.middletier.backup.dto;

import lombok.Data;

@Data
public class LocationDTO {
    private Integer id;
    private String name;
    private String size;
    private Boolean checkDuplicates;

    public LocationDTO() {
        setId(0);
        setName("");
        setSize("");
    }
}
