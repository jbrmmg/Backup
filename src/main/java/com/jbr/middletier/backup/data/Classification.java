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

    @Column(name="manual")
    private boolean manual;

    @Column(name="remove")
    private boolean remove;

    @Column(name="backup")
    private boolean backup;

    public boolean fileMatches(File file) {
        return file.getName().matches(regex);
    }
}
