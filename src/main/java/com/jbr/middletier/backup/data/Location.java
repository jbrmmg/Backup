package com.jbr.middletier.backup.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

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

    public int getId() { return this.id; }

    public String getName() { return this.name; }

    public String getSize() { return this.size; }
}
