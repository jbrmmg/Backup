package com.jbr.middletier.backup.data;

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
    @NotNull
    private Integer id;

    @Column(name="name")
    @NotNull
    private String name;

    @Column(name="size")
    @NotNull
    private String size;

    @Column(name="check_duplicates")
    private Boolean checkDuplicates;

    public int getId() { return this.id; }

    public String getName() { return this.name; }

    public String getSize() { return this.size; }

    public Boolean getCheckDuplicates() { return this.checkDuplicates; }

    public void setId(Integer id) { this.id = id; }

    public void setName(String name) { this.name = name; }

    public void setSize(String size) { this.size = size; }

    public void setCheckDuplicates(Boolean checkDuplicates) { this.checkDuplicates = checkDuplicates; }

    @Override
    public String toString() {
        return name;
    }
}
