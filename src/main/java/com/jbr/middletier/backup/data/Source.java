package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="source")
public class Source {
    @Id
    @Column(name="id")
    @NotNull
    private Integer id;

    @Column(name="path")
    @NotNull
    private String path;

    @JoinColumn(name="location")
    @ManyToOne(optional = false)
    private Location location;

    public void setPath(String path) { this.path = path; }

    public String getPath() { return this.path; }

    public int getId() { return this.id; }

    @Override
    public String toString() {
        return path;
    }
}
