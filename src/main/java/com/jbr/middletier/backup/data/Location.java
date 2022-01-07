package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.LocationDTO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
@Entity
@Table(name="location")
public class Location {
    @Id
    @Column(name="id")
    private Integer id;

    @Column(name="name")
    private String name;

    @Column(name="size")
    private String size;

    @Column(name="check_duplicates")
    private Boolean checkDuplicates;

    public Location() {
        setId(0);
        setName("");
        setSize("");
    }

    public Location(LocationDTO source) {
        setId(source.getId());
        setName(source.getName());
        setSize(source.getSize());
        update(source);
    }

    public void update(LocationDTO source) {
        setName(source.getName());
        setSize(source.getSize());
        this.checkDuplicates = source.getCheckDuplicates();
    }

    public LocationDTO getLocationDTO() {
        LocationDTO result = new LocationDTO();

        result.setId(getId());
        result.setName(getName());
        result.setSize(getSize());
        result.setCheckDuplicates(getCheckDuplicates());

        return result;
    }

    public int getId() { return this.id; }

    public String getName() { return this.name; }

    public String getSize() { return this.size; }

    public Boolean getCheckDuplicates() { return this.checkDuplicates; }

    public void setId(@NotNull Integer id) { this.id = id; }

    public void setName(@NotNull String name) { this.name = name; }

    public void setSize(@NotNull String size) { this.size = size; }

    @Override
    public String toString() {
        return name;
    }
}
