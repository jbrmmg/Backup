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

    public int getId() { return this.id; }

    protected void setId(int id) { this.id = id; }

    protected String getType() { return this.type; }

    protected String setType(@NotNull String type) { return this.type; }

    public FileSystemObject getParent() {
        return parent;
    }

    public void setParent(FileSystemObject parent) {
        this.parent = parent;
    }
}
