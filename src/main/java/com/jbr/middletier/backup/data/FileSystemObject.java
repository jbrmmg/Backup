package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@SuppressWarnings({"unused", "WeakerAccess"})
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

    public void setId(Integer id) { this.id = id; }

    public Optional<FileSystemObjectId> getParentId() {
        if(this.parentId == null) {
            return Optional.empty();
        }

        return Optional.of(new FileSystemObjectId(this.parentId, FileSystemObjectType.getFileSystemObjectType(this.parentType)));
    }

    public void setParent(Optional<FileSystemObject> parent) {
        if(!parent.isPresent()) {
            setParentId(Optional.empty());
            return;
        }

        setParentId(Optional.of(parent.get().getIdAndType()));
    }

    public void setParentId(Optional<FileSystemObjectId> parentId) {
        if(!parentId.isPresent()) {
            this.parentId = null;
            this.parentType = null;
            return;
        }

        this.parentId = parentId.get().getId();
        this.parentType = parentId.get().getType().getTypeName();
    }

    public String getName() {
        return this.name;
    }
}
