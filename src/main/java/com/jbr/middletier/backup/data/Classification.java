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

    public Integer getId() { return this.id; }

    public void setId(Integer id) { this.id = id; }

    public String getRegex() { return this.regex; }

    public void setRegex(String regex) { this.regex = regex; }

    public ClassificationActionType getAction() { return ClassificationActionType.getClassificationActionType(this.action); }

    public void setAction(ClassificationActionType action) { this.action = action.getTypeName(); }

    public String getIcon() { return this.icon; }

    public void setIcon(String icon) { this.icon = icon; }

    @NotNull public Boolean getUseMD5() { return this.useMD5; }

    public void setUseMD5(@NotNull Boolean useMD5) { this.useMD5 = useMD5; }

    public Boolean getIsVideo() { return this.isVideo; }

    public void setIsVideo(Boolean isVideo) { this.isVideo = isVideo; }

    public Boolean getIsImage() { return this.isImage; }

    public void setIsImage(Boolean isImage) { this.isImage = isImage; }

    public @NotNull Integer getOrder() { return this.order; }

    public void setOrder(@NotNull Integer order) { this.order = order; }

    @Override
    public String toString() {
        return id + "-" + regex;
    }

    public boolean fileMatches(FileInfo file) {
        return file.getName().toLowerCase().matches(regex);
    }
}
