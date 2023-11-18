package com.jbr.middletier.backup.data;

import javax.persistence.*;

@Entity
@Table(name="size")
public class PrintSize {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="name")
    private String name;

    @Column(name="width_in")
    private Double width;

    @Column(name="height_in")
    private Double height;

    @Column(name="retro")
    private Boolean retro;

    @Column(name="panoramic")
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
