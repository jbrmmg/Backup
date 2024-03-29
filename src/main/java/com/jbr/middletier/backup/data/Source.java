package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.Optional;

@SuppressWarnings({"unused", "DefaultAnnotationParam", "WeakerAccess"})
@Entity
@Table(name="source")
@Inheritance(strategy = InheritanceType.JOINED)
public class Source extends FileSystemObject {
    @JoinColumn(name="location")
    @ManyToOne(optional = true)
    private Location location;

    @Column(name="status")
    private String status;

    @Column(name="filter")
    private String filter;

    @Column(name="mount_check")
    private String mountCheck;

    protected Source(FileSystemObjectType sourceType) {
        super(sourceType);
    }

    public Source() {
        super(FileSystemObjectType.FSO_SOURCE);
        setPath("");
    }

    public void setPath(@NotNull String path) { this.name = path; }

    public void setStatus(SourceStatusType status) { this.status = status.getTypeName(); }

    public void setFilter(String filter) { this.filter = filter; }

    public SourceStatusType getStatus() { return SourceStatusType.getSourceStatusType(this.status); }

    public String getPath() { return this.name; }

    public String getFilter() { return this.filter; }

    public Location getLocation() { return this.location; }

    public void setLocation(Location location) { this.location = location; }

    public Optional<File> getMountCheck() {
        if(this.mountCheck == null) {
            return Optional.empty();
        }

        return Optional.of(new File(this.mountCheck));
    }

    public void setMountCheck(String mountCheck) {
        this.mountCheck = mountCheck;
    }

    @Override
    public String toString() {
        return name;
    }
}
