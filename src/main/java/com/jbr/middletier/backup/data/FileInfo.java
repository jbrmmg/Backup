package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Optional;

@SuppressWarnings({"DefaultAnnotationParam"})
@Entity
@Table(name="file")
@Inheritance(strategy = InheritanceType.JOINED)
public class FileInfo extends FileSystemObject {
    @Getter
    @Setter
    @JoinColumn(name="classificationId")
    @ManyToOne(optional = true)
    private Classification classification;

    @Getter
    @Setter
    @Column(name="date")
    private LocalDateTime date;

    @Getter
    @Column(name="size")
    private Long size;

    @Column(name="md5")
    private String md5;

    @Setter
    @Getter
    @Column(name="expiry")
    private LocalDateTime expiry;

    @Getter
    @Transient
    private boolean md5Regenerated;

    public FileInfo() {
        super(FileSystemObjectType.FSO_FILE);
        this.md5Regenerated = false;
    }

    protected FileInfo(@NotNull FileSystemObjectType type) {
        super(type);
        this.md5Regenerated = false;
    }

    public void setName(String name) { this.name = name; }

    public void setSize(long size) { this.size = size; }

    public void setMd5(MD5 md5) {
        this.md5Regenerated = true;
        this.md5 = md5 != null ? md5.toString() : null;
    }

    public Optional<MD5> getMd5() { return this.md5 == null ? Optional.empty() : Optional.of(new MD5(this.md5)); }

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

    public boolean matchClassification(Classification classification) {
        return this.getName().toLowerCase().matches(classification.getRegex());
    }

    @Override
    public String toString() {
        return "FileInfo: " + getIdAndType().toString() + " " + getName() + " " + md5;
    }
}
