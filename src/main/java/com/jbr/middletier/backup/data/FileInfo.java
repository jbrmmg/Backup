package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
@Entity
@Table(name="file")
@Inheritance(strategy = InheritanceType.JOINED)
public class FileInfo extends FileSystemObject {
    @JoinColumn(name="classificationId")
    @ManyToOne(optional = true)
    private Classification classification;

    @Column(name="date")
    private LocalDateTime date;

    @Column(name="size")
    private Long size;

    @Column(name="md5")
    private String md5;

    @Column(name="flags")
    private String flags;

    @Column(name="expiry")
    private LocalDateTime expiry;

    public FileInfo() {
        super(FileSystemObjectType.FSO_FILE);
    }

    protected FileInfo(@NotNull FileSystemObjectType type) {
        super(type);
    }

    public void setName(String name) { this.name = name; }

    public void setClassification(Classification classification) { this.classification = classification; }

    public void setSize(long size) { this.size = size; }

    public void setDate(LocalDateTime date) { this.date = date; }

    public void setMD5(MD5 md5) { this.md5 = md5.toString().isEmpty() ? null : md5.toString(); }

    public Long getSize() { return this.size; }

    public LocalDateTime getDate() { return this.date; }

    public MD5 getMD5() { return new MD5(this.md5); }

    public Classification getClassification() { return this.classification; }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public LocalDateTime getExpiry() {
        return expiry;
    }

    public void setExpiry(LocalDateTime expiry) {
        this.expiry = expiry;
    }

    public boolean duplicate(@org.jetbrains.annotations.NotNull FileInfo otherFile) {
        if(this.getIdAndType().equals(otherFile.getIdAndType())) {
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

    @Override
    public String toString() {
        return "FileInfo: " + getIdAndType().toString() + " " + getName() + " " + md5;
    }
}
