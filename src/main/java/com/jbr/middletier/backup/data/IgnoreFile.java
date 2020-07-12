package com.jbr.middletier.backup.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@SuppressWarnings("unused")
@Entity
@Table(name="ignore_file")
public class IgnoreFile {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="name")
    @NotNull
    private String name;

    @Column(name="date")
    private Date date;

    @Column(name="size")
    private Long size;

    @Column(name="md5")
    private String md5;

    public void setId(Integer id) { this.id = id; }

    public Integer getId() { return this.id; }

    public void setName(String name) { this.name = name; }

    public String getNmae() { return this.name; }

    public void setDate(Date date) { this.date = date; }

    public Date getDate() { return this.date; }

    public void setSize(Long size) { this.size = size; }

    public Long getSize() { return this.size; }

    public void setMD5(String md5) { this.md5 = md5; }

    public String getMD5() { return this.md5; }
}
