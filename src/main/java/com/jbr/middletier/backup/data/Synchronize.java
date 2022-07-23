package com.jbr.middletier.backup.data;

import org.jetbrains.annotations.NotNull;

import javax.persistence.*;

@Entity
@Table(name="synchronize")
public class Synchronize {
    @Id
    @Column(name="id")
    private Integer id;

    @JoinColumn(name="source")
    @ManyToOne(optional = false)
    private Source source;

    @JoinColumn(name="destination")
    @ManyToOne(optional = false)
    private Source destination;

    public Synchronize() {
        setId(0);
    }

    @NotNull public Integer getId() { return this.id; }

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
