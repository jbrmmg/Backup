package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

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
    private boolean removed;

    public void setSource(Source source) { this.source = source; }

    public void clearRemoved() { this.removed = false; }

    public void setPath(String path) { this.path = path; }
}
