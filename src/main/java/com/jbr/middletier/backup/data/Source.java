package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.SourceDTO;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

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

    @Column(name="source_type")
    private String sourceType;

    protected Source(FileSystemObjectType sourceType) {
        super(sourceType);
    }

    public Source() {
        super(FileSystemObjectType.FSO_SOURCE);
        this.sourceType = "STD";
        setPath("");
    }

    public Source(String path) {
        this();
        setPath(path);
    }

    public Source(SourceDTO source) {
        this();
        update(source);
    }

    public void update(SourceDTO source) {
        setPath(source.getPath());
        setLocation(new Location(source.getLocation()));
        setStatus(source.getStatus());
        setFilter(source.getFilter());
    }

    public SourceDTO getSourceDTO() {
        SourceDTO result = new SourceDTO();

        result.setId(getIdAndType().getId());
        result.setLocation(getLocation().getLocationDTO());
        result.setFilter(getFilter());
        result.setStatus(getStatus());
        result.setPath(getPath());

        return result;
    }

    public void setPath(@NotNull String path) { this.name = path; }

    public void setStatus(SourceStatusType status) { this.status = status.getTypeName(); }

    public void setFilter(String filter) { this.filter = filter; }

    public SourceStatusType getStatus() { return SourceStatusType.getSourceStatusType(this.status); }

    public String getPath() { return this.name; }

    public String getFilter() { return this.filter; }

    public Location getLocation() { return this.location; }

    public void setLocation(Location location) { this.location = location; }

    @Override
    public String toString() {
        return name;
    }
}
