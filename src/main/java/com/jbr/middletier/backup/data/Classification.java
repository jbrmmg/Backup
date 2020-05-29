package com.jbr.middletier.backup.data;

import javax.persistence.*;

@Entity
@Table(name="classification")
public class Classification {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="regex")
    private String regex;

    @Column(name="action")
    private String action;

    public boolean fileMatches(FileInfo file) {
        return file.getName().toLowerCase().matches(regex);
    }
}
