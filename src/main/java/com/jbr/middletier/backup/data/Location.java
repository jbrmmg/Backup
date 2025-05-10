package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@SuppressWarnings("unused")
@Entity
@Table(name="location")
public class Location {
    @Id
    @Column(name="id")
    private Integer id;

    @Getter
    @Column(name="name")
    private String name;

    @Getter
    @Column(name="size")
    private String size;

    @Setter
    @Getter
    @Column(name="check_duplicates")
    private Boolean checkDuplicates;

    public Location() {
        setId(0);
        setName("");
        setSize("");
    }

    public int getId() { return this.id; }

    public void setId(@NotNull Integer id) { this.id = id; }

    public void setName(@NotNull String name) { this.name = name; }

    public void setSize(@NotNull String size) { this.size = size; }

    @Override
    public String toString() {
        return name;
    }
}
