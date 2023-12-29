package com.jbr.middletier.backup.dto;

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

    public Double getWidth() {
        return width;
    }

    public String getName() {
        return name;
    }

    public Double getHeight() {
        return height;
    }

    public Boolean getRetro() {
        return retro;
    }

    public Boolean getPanoramic() {
        return panoramic;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public void setRetro(Boolean retro) {
        this.retro = retro;
    }

    public void setPanoramic(Boolean panoramic) {
        this.panoramic = panoramic;
    }
}
