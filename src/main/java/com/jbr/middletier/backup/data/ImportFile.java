package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Table(name="import_file")
public class ImportFile {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="filename")
    @NotNull
    private String name;

    @Column(name="path")
    @NotNull
    private String path;

    @Column(name="date")
    private Date date;

    @Column(name="size")
    private Long size;

    @Column(name="md5")
    private String md5;

    @Column(name="status")
    private String status;

    @Column(name="to_source")
    private Integer to;

    public void setName(String name) { this.name = name; }

    public String getName() { return this.name; }

    public void setPath(String path) { this.path = path; }

    public String getPath() { return this.path; }

    public void setDate(Date date) { this.date = date; }

    public void setTo(int to) { this.to = to; }

    public int getTo() { return this.to; }

    public Date getDate() { return this.date; }

    public void setSize(Long size) { this.size = size; }

    public void setStatus(String status) { this.status = status; }

    public String getStatus() { return this.status; }

    public Long getSize() { return this.size; }

    public void setMD5(String md5) { this.md5 = md5; }

    public String getMD5() { return this.md5; }
}
