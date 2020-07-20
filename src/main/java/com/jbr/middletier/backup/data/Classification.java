package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.dto.ClassificationDTO;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;

@SuppressWarnings("unused")
@Entity
@Table(name="classification")
public class Classification {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="\"order\"")
    @NotNull
    private Integer order;

    @Column(name="regex")
    private String regex;

    @Column(name="action")
    private String action;

    @Column(name="icon")
    private String icon;

    @Column(name="useMD5")
    @NotNull
    private Boolean useMD5;

    @Column(name="is_image")
    private Boolean isImage;

    @Column(name="is_video")
    private Boolean isVideo;

    public Classification() {
        this.order = 0;
        this.useMD5 = false;
    }

    public Classification(ClassificationDTO source) {
        this.id = source.getId();
        this.order = source.getOrder();
        this.useMD5 = source.getUseMD5();
        update(source);
    }

    public void update(ClassificationDTO source) {
        this.regex = source.getRegex();
        this.action = source.getAction();
        this.icon = source.getIcon();
        this.useMD5 = source.getUseMD5();
        this.isImage = source.getImage();
        this.isVideo = source.getVideo();
        this.order = source.getOrder();

        // For future use.
        String type = source.getType();
    }

    public Integer getId() { return this.id; }

    public String getRegex() { return this.regex; }

    public String getAction() { return this.action; }

    public String getIcon() { return this.icon; }

    @NotNull
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
