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

    @Column(name="parent")
    private Integer parentId;

    @Column(name="parent_type")
    private String parentType;

    @Transient
    private final FileSystemObjectType fileSystemObjectType;

    protected FileSystemObject() {
        this.fileSystemObjectType = null;
        this.type = "UNK";
    }

    protected FileSystemObject(@NotNull FileSystemObjectType type) {
        this.fileSystemObjectType = type;
        this.type = type.getTypeName();
    }

    public FileSystemObjectId getIdAndType() {
        return new FileSystemObjectId(this.id, FileSystemObjectType.getFileSystemObjectType(this.type));
    }

    protected void setId(int id) { this.id = id; }

    public FileSystemObjectId getParentId() {
        if(this.parentId == null) {
            return null;
        }

        return new FileSystemObjectId(this.parentId, FileSystemObjectType.getFileSystemObjectType(this.parentType));
    }

    public void setParentId(FileSystemObject parent) {
        if(parent == null) {
            this.id = null;
            this.type = null;
        }

        assert parent != null;
        FileSystemObjectId parentId = parent.getIdAndType();
        this.parentId = parentId.getId();
        this.parentType = parentId.getType().getTypeName();
    }
}
