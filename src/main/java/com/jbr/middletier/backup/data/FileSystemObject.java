package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@SuppressWarnings({"unused", "DefaultAnnotationParam", "WeakerAccess"})
@Entity
@Table(name="file_system_object")
@Inheritance(strategy = InheritanceType.JOINED)
public class FileSystemObject {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="type")
    private String type;

    @Column(name="name")
    protected String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="parent", nullable = true)
    private FileSystemObject parent;

    protected FileSystemObject(@NotNull String type) {
        this.type = type;
    }

    public Integer getId() {
        return this.id;
    }

    protected void setId(int id) { this.id = id; }

    public FileSystemObject getParent() {
        return parent;
    }

    public void setParent(FileSystemObject parent) {
        this.parent = parent;
    }
}
