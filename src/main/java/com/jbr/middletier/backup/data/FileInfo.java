package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Table(name="file")
public class FileInfo {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="name")
    @NotNull
    private String name;

    @JoinColumn(name="directoryId")
    @ManyToOne(optional = false)
    private DirectoryInfo directoryInfo;

    @JoinColumn(name="classificationId")
    @ManyToOne(optional = true)
    private Classification classification;

    @Column(name="date")
    private Date date;

    @Column(name="size")
    private Long size;

    @Column(name="removed")
    @NotNull
    private boolean removed;

    public void setName(String name) { this.name = name; }

    public void setDirectoryInfo(DirectoryInfo directoryInfo) { this.directoryInfo = directoryInfo; }

    public void setClassification(Classification classification) { this.classification = classification; }

    public void setSize(long size) { this.size = size; }

    public void setDate(Date date) { this.date = date; }

    public void clearRemoved() { this.removed = false; }

    public String getName() { return this.name; }

    public Classification getClassification() { return this.classification; }
}
