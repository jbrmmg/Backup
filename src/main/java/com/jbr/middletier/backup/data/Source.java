package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.SourceDTO;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@SuppressWarnings({"unused", "DefaultAnnotationParam", "WeakerAccess"})
@Entity
@Table(name="source")
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

    @Column(name="destination")
    private Integer destinationId;

    public enum SourceTypeType { STANDARD, IMPORT }

    public Source() {
        super("SRCE");
        setPath("");
    }

    public Source(String path) {
        this();
        setPath(path);
        setTypeEnum(SourceTypeType.STANDARD);
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
        setSourceType(source.getType());
        setDestinationId(source.getDestinationId());
    }

    public SourceDTO getSourceDTO() {
        SourceDTO result = new SourceDTO();

        result.setId(getId());
        result.setType(getSourceType());
        result.setDestinationId(getDestinationId());
        result.setLocation(getLocation().getLocationDTO());
        result.setFilter(getFilter());
        result.setStatus(getStatus());
        result.setPath(getPath());

        return result;
    }

    public void setPath(@NotNull String path) { this.name = path; }

    public void setStatus(String status) { this.status = status; }

    public void setFilter(String filter) { this.filter = filter; }

    public String getStatus() { return this.status; }

    public String getPath() { return this.name; }

    public String getFilter() { return this.filter; }

    public String getSourceType() { return this.sourceType; }

    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public SourceTypeType getTypeEnum() {
        switch(this.getSourceType()) {
            case "STD":
                return SourceTypeType.STANDARD;
            case "IMP":
                return SourceTypeType.IMPORT;
            default:
                throw new IllegalArgumentException(this.getSourceType() + " invalid type");
        }
    }

    public void setTypeEnum(SourceTypeType type) {
        if(SourceTypeType.IMPORT == type) {
            this.sourceType = "IMP";
            return;
        }

        // Must be standard.
        this.sourceType = "STD";
    }


    public Location getLocation() { return this.location; }

    public Integer getDestinationId() { return this.destinationId; }

    public void setDestinationId(Integer id) { this.destinationId = id; }

    public void setLocation(Location location) { this.location = location; }

    @Override
    public String toString() {
        return name;
    }
}
