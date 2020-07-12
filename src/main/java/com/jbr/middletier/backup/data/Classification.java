package com.jbr.middletier.backup.data;

import javax.persistence.*;

@SuppressWarnings("unused")
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

    @Column(name="icon")
    private String icon;

    @Column(name="useMD5")
    private Boolean useMD5;

    @Column(name="type")
    private String type;

    @Column(name="is_image")
    private Boolean isImage;

    @Column(name="is_video")
    private Boolean isVideo;

    public Integer getId() { return this.id; }

    public String getRegex() { return this.regex; }

    public String getAction() { return this.action; }

    public String getIcon() { return this.icon; }

    public Boolean getUseMD5() { return this.useMD5; }

    public Boolean getIsVideo() { return this.isVideo; }

    public Boolean getIsImage() { return this.isImage; }

    public boolean fileMatches(FileInfo file) {
        return file.getName().toLowerCase().matches(regex);
    }

    @Override
    public String toString() {
        return id + "-" + regex;
    }
}
