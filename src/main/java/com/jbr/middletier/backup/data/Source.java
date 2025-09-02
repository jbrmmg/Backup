package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.util.Optional;

@Entity
@Table(name="source")
@Inheritance(strategy = InheritanceType.JOINED)
public class Source extends FileSystemObject {
    @Setter
    @Getter
    @JoinColumn(name="location")
    @ManyToOne
    private Location location;

    @Column(name="status")
    private String status;

    // NOTE: this causes problems for h2 unit tests.
    @Getter
    @Setter
    @Column(name="filter")
    private String filter;

    @Setter
    @Column(name="mount_check")
    private String mountCheck;

    @Setter
    @Column(name="gather_meta_data")
    private Boolean gatherMetaData;

    @Setter
    @Column(name="primary_source")
    private Boolean primary;

    protected Source(FileSystemObjectType sourceType) {
        super(sourceType);
    }

    public Source() {
        super(FileSystemObjectType.FSO_SOURCE);
        setPath("");
    }

    public void setPath(@NotNull String path) { this.name = path; }

    public void setStatus(SourceStatusType status) { this.status = status.getTypeName(); }

    public SourceStatusType getStatus() { return SourceStatusType.getSourceStatusType(this.status); }

    public String getPath() { return this.name; }

    public boolean getGatherMetaData() {
        return gatherMetaData != null && gatherMetaData;
    }

    public Optional<File> getMountCheck() {
        if(this.mountCheck == null) {
            return Optional.empty();
        }

        return Optional.of(new File(this.mountCheck));
    }

    public boolean getPrimary() {
        // Default is false.
        return primary != null && primary;
    }

    @Override
    public String toString() {
        return name;
    }
}
