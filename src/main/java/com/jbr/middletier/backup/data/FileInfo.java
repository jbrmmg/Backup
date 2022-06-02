package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
@Entity
@Table(name="file")
public class FileInfo extends FileSystemObject {
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

    public FileInfo() {
        super(FileSystemObjectType.FSO_FILE);
    }

    public void setName(String name) { this.name = name; }

    public void setClassification(Classification classification) { this.classification = classification; }

    public void setSize(long size) { this.size = size; }

    public void setDate(Date date) { this.date = date; }

    public void setMD5(String md5) { this.md5 = md5; }

    public void clearRemoved() { this.removed = false; }

    public String getName() { return this.name; }

    public Long getSize() { return this.size; }

    public Date getDate() { return this.date; }

    public String getMD5() { return this.md5; }

    public Classification getClassification() { return this.classification; }

    public Boolean getRemoved() { return this.removed; }

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

    public String getFullFilename() {
        List<String> result = new ArrayList<>();

        result.add(getName());

        // TODO fix this
//        FileSystemObject parent = getDirectoryInfo().getParent();
//        while(parent != null) {
//            result.add(parent.name);
//            parent = parent.getParent();
//        }

        Collections.reverse(result);

        return String.join("/", result);
    }

    @Override
    public String toString() {
        return "FileInfo: " + getIdAndType().toString() + " " + getFullFilename() + " " + md5;
    }
}
