package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.SourceDTO;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
@Entity
@Table(name="source")
public class Source {
    @Id
    @Column(name="id")
    private Integer id;

    @Column(name="path")
    private String path;

    @JoinColumn(name="location")
    @ManyToOne(optional = true)
    private Location location;

    @Column(name="status")
    private String status;

    @Column(name="filter")
    private String filter;

    @Column(name="source_type")
    private String type;

    @Column(name="destination")
    private Integer destinationId;

    public enum SourceTypeType { Standard, Import }

    public Source() {
        setId(0);
        setPath("");
    }

    public Source(int id, String path) {
        setId(id);
        setPath(path);
        setTypeEnum(SourceTypeType.Standard);
    }

    public Source(SourceDTO source) {
        setId(source.getId());
        setPath(source.getPath());
        update(source);
    }

    public void update(SourceDTO source) {
        setPath(source.getPath());
        setLocation(new Location(source.getLocation()));
        setStatus(source.getStatus());
        setFilter(source.getFilter());
        setType(source.getType());
        setDestinationId(source.getDestinationId());
    }

    public void setPath(@NotNull String path) { this.path = path; }

    public void setStatus(String status) { this.status = status; }

    public void setFilter(String filter) { this.filter = filter; }

    public String getStatus() { return this.status; }

    public String getPath() { return this.path; }

    public String getFilter() { return this.filter; }

    public String getType() { return this.type; }

    public void setType(String type) { this.type = type; }

    public SourceTypeType getTypeEnum() throws Exception {
        switch(this.getType()) {
            case "STD":
                return SourceTypeType.Standard;
            case "IMP":
                return SourceTypeType.Import;
            default:
                throw new Exception(this.getType() + " invalid type");
        }
    }

    public void setTypeEnum(SourceTypeType type) {
        switch(type) {
            case Standard:
                this.type = "STD";
                break;
            case Import:
                this.type = "IMP";
                break;
        }
    }


    public Location getLocation() { return this.location; }

    public int getId() { return this.id; }

    public void setId(@NotNull Integer id) { this.id = id; }

    public Integer getDestinationId() { return this.destinationId; }

    public void setDestinationId(Integer id) { this.destinationId = id; }

    public void setLocation(Location location) { this.location = location; }

    @Override
    public String toString() {
        return path;
    }
}
