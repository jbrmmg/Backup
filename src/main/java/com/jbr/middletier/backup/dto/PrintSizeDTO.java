package com.jbr.middletier.backup.dto;

import javax.persistence.Column;

public class PrintSizeDTO {
    private Integer id;

    private String name;

    private Double width;

    private Double height;

    private Boolean retro;

    private Boolean panoramic;

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

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Boolean getRetro() {
        return retro;
    }

    public void setRetro(Boolean retro) {
        this.retro = retro;
    }

    public Boolean getPanoramic() {
        return panoramic;
    }

    public void setPanoramic(Boolean panoramic) {
        this.panoramic = panoramic;
    }
}
