package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
@Entity
@Table(name="directory")
public class DirectoryInfo {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "directory_seq")
    private Integer id;

    @Column(name="path")
    @NotNull
    private String name;

    @JoinColumn(name="sourceId")
    @ManyToOne(optional = false)
    private Source source;

    @Column(name="removed")
    @NotNull
    private Boolean removed;

    @JoinColumn(name="parent_id")
    @ManyToOne
    private DirectoryInfo parent;

    public String getName() { return this.name; }

    public Source getSource() { return this.source; }

    public Integer getId() { return this.id; }

    public Boolean getRemoved() { return this.removed; }

    public void setSource(Source source) { this.source = source; }

    public void clearRemoved() { this.removed = false; }

    public void setName(String name) { this.name = name; }

    public DirectoryInfo getParent() {
        return parent;
    }

    public void setParent(DirectoryInfo parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "DirectoryInfo: " + id + "-" + name + "-" + parent.getId();
    }
}
