package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    @Setter
    @Getter
    private LocalDateTime startTime;

    @Setter
    @Getter
    private LocalDateTime endTime;

    @Setter
    @Getter
    private Integer filesCopied;

    @Setter
    @Getter
    private Integer directoriesCopied;

    @Setter
    @Getter
    private Integer filesDeleted;

    @Setter
    @Getter
    private Integer directoriesDeleted;

    @Setter
    @Getter
    private Integer sourcesRemoved;

    @Setter
    @Getter
    private Integer datesUpdated;

    @Setter
    @Getter
    private Integer filesWarned;

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
