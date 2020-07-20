package com.jbr.middletier.backup.dto;

@SuppressWarnings("unused")
public class LocationDTO {
    private Integer id;
    private String name;
    private String size;
    private Boolean checkDuplicates;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Boolean getCheckDuplicates() {
        return checkDuplicates;
    }

    public void setCheckDuplicates(Boolean checkDuplicates) {
        this.checkDuplicates = checkDuplicates;
    }
}
