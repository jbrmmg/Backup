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

    @Column(name="status")
    private String status;

    @Column(name="filter")
    private String filter;

    @Column(name="auto_gather")
    private Boolean autoGather;

    public void setPath(String path) { this.path = path; }

    public void setStatus(String status) { this.status = status; }

    public String getStatus() { return this.status; }

    public String getPath() { return this.path; }

    public String getFilter() { return this.filter; }

    public boolean getAutoGather() { return this.autoGather == null ? false : this.autoGather; }

    public Location getLocation() { return this.location; }

    public int getId() { return this.id; }

    @Override
    public String toString() {
        return path;
    }
}
