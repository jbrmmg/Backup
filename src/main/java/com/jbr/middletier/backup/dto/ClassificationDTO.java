package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.ClassificationActionType;

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

    public Boolean getIsImage() {
        return isImage;
    }

    public void setIsImage(Boolean image) {
        isImage = image;
    }

    public Boolean getIsVideo() {
        return isVideo;
    }

    public void setIsVideo(Boolean video) {
        isVideo = video;
    }
}
