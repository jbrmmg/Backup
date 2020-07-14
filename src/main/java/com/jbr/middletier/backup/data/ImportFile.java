package com.jbr.middletier.backup.data;

import javax.persistence.*;

@Entity
@Table(name="import_file")
public class ImportFile {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JoinColumn(name="fileId")
    @ManyToOne(optional = false)
    private FileInfo fileInfo;

    @Column(name="status")
    private String status;

    public void setId(Integer id) { this.id = id; }

    public Integer getId() { return this.id; }

    public void setStatus(String status) { this.status = status; }

    public String getStatus() { return this.status; }

    public void setFileInfo(FileInfo file) { this.fileInfo = file; }

    public FileInfo getFileInfo() { return this.fileInfo; }
}
