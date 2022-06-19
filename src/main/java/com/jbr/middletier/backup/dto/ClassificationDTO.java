package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.Classification;
import com.jbr.middletier.backup.data.ClassificationActionType;
import com.jbr.middletier.backup.data.FileInfo;

@SuppressWarnings("unused")
public class ClassificationDTO {
    private Integer id;
    private Integer order;
    private String regex;
    private ClassificationActionType action;
    private String icon;
    private Boolean useMD5;
    private Boolean isImage;
    private Boolean isVideo;

    public ClassificationDTO() {
    }

    public ClassificationDTO(Classification classification) {
        this.id = classification.getId();
        this.order = classification.getOrder();
        this.regex = classification.getRegex();
        this.action = classification.getAction();
        this.icon = classification.getIcon();
        this.useMD5 = classification.getUseMD5();
        this.isImage = classification.getIsImage();
        this.isVideo = classification.getIsVideo();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public ClassificationActionType getAction() {
        return action;
    }

    public void setAction(ClassificationActionType action) {
        this.action = action;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Boolean getUseMD5() {
        return useMD5;
    }

    public void setUseMD5(Boolean useMD5) {
        this.useMD5 = useMD5;
    }

    public Boolean getImage() {
        return isImage;
    }

    public void setImage(Boolean image) {
        isImage = image;
    }

    public Boolean getVideo() {
        return isVideo;
    }

    public void setVideo(Boolean video) {
        isVideo = video;
    }
}
