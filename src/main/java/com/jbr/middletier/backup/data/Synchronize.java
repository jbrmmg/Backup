package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.SynchronizeDTO;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;

@Entity
@Table(name="synchronize")
public class Synchronize {
    @Id
    @Column(name="id")
    @NotNull
    private Integer id;

    @JoinColumn(name="source")
    @ManyToOne(optional = false)
    private Source source;

    @JoinColumn(name="destination")
    @ManyToOne(optional = false)
    private Source destination;

    @SuppressWarnings("unused")
    public Synchronize() {
        this.id = 0;
    }

    public Synchronize(SynchronizeDTO source) {
        this.id = source.getId();
        update(source);
    }

    public void update(SynchronizeDTO source) {
        this.source = new Source(source.getSource());
        this.destination = new Source(source.getDestination());
    }

    @NotNull
    public Integer getId() { return this.id; }

    public Source getSource() { return this.source; }

    public Source getDestination() { return this.destination; }

    public void setId(@NotNull Integer id) { this.id = id; }

    public void setSource(Source source) { this.source = source; }

    public void setDestination(Source destination) { this.destination = destination; }

    @Override
    public String toString() {
        return source.toString() +" -> " + destination.toString();
    }
}
