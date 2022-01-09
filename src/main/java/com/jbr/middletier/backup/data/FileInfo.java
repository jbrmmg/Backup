package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
@Entity
@Table(name="file")
public class FileInfo {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_seq")
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
    private Boolean removed;

    @Column(name="md5")
    private String md5;

    @Column(name="flags")
    private String flags;

    public void setName(String name) { this.name = name; }

    public void setDirectoryInfo(DirectoryInfo directoryInfo) { this.directoryInfo = directoryInfo; }

    public void setClassification(Classification classification) { this.classification = classification; }

    public void setSize(long size) { this.size = size; }

    public void setDate(Date date) { this.date = date; }

    public void setMD5(String md5) { this.md5 = md5; }

    public void clearRemoved() { this.removed = false; }

    public Integer getId() { return this.id; }

    public String getName() { return this.name; }

    public Long getSize() { return this.size; }

    public Date getDate() { return this.date; }

    public String getMD5() { return this.md5; }

    public DirectoryInfo getDirectoryInfo() { return this.directoryInfo; }

    public Classification getClassification() { return this.classification; }

    public Boolean getRemoved() { return this.removed; }

    public boolean duplicate(FileInfo otherFile) {
        if(this.id.equals(otherFile.id)) {
            return false;
        }

        if(!this.name.equals(otherFile.name)) {
            return false;
        }

        if((this.size != null) && (otherFile.size != null) && !this.size.equals(otherFile.size)) {
            return false;
        }

        return (this.md5 == null) || (otherFile.md5 == null) || this.md5.equals(otherFile.md5);
    }

    public String getFullFilename() {
        return directoryInfo.getSource().getPath() +
                directoryInfo.getPath() +
                "/" +
                getName();
    }

    @Override
    public String toString() {
        return "FileInfo: " + id + getFullFilename() + " " + md5;
    }
}
