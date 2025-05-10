package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import jakarta.persistence.*;

@Entity
@Table(name="synchronize")
public class Synchronize {
    @Id
    @Column(name="id")
    private Integer id;

    @Setter
    @Getter
    @JoinColumn(name="source")
    @ManyToOne(optional = false)
    private Source source;

    @Setter
    @Getter
    @JoinColumn(name="destination")
    @ManyToOne(optional = false)
    private Source destination;

    public Synchronize() {
        setId(0);
    }

    @NotNull public Integer getId() { return this.id; }

    public void setId(@NotNull Integer id) { this.id = id; }

    @Override
    public String toString() {
        return source.toString() +" -> " + destination.toString();
    }
}
