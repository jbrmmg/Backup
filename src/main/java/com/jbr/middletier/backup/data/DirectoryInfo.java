package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
@Entity
@Table(name="directory")
public class DirectoryInfo {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="path")
    @NotNull
    private String path;

    @JoinColumn(name="sourceId")
    @ManyToOne(optional = false)
    private Source source;

    @Column(name="removed")
    @NotNull
    private Boolean removed;

    public String getPath() { return this.path; }

    public Source getSource() { return this.source; }

    public Integer getId() { return this.id; }

    public Boolean getRemoved() { return this.removed; }

    public void setSource(Source source) { this.source = source; }

    public void clearRemoved() { this.removed = false; }

    public void setPath(String path) { this.path = path; }

    @Override
    public String toString() {
        return "DirectoryInfo: " + id + "-" + path;
    }
}
