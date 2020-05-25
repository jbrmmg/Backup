package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="file")
public class File {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="name")
    @NotNull
    private String name;

    @JoinColumn(name="directoryId")
    @ManyToOne(optional = false)
    private Directory directory;

    @JoinColumn(name="classificationId")
    @ManyToOne(optional = true)
    private Classification classification;

    @Column(name="removed")
    @NotNull
    private boolean removed;

    public void setName(String name) { this.name = name; }

    public void setDirectory(Directory directory) { this.directory = directory; }

    public void setClassification(Classification classification) { this.classification = classification; }

    public String getName() { return this.name; }

    public Classification getClassification() { return this.classification; }
}
