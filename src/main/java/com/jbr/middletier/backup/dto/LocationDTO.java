package com.jbr.middletier.backup.dto;

import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
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

    public Integer getId() {
        return id;
    }

    public void setId(@NotNull Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(@NotNull String size) {
        this.size = size;
    }

    public Boolean getCheckDuplicates() {
        return checkDuplicates;
    }

    public void setCheckDuplicates(Boolean checkDuplicates) {
        this.checkDuplicates = checkDuplicates;
    }
}
