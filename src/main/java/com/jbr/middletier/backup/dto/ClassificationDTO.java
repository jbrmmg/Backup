package com.jbr.middletier.backup.dto;

@SuppressWarnings("unused")
public class ClassificationDTO {
    private Integer id;
    private Integer order;
    private String regex;
    private String action;
    private String icon;
    private Boolean useMD5;
    private String type;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
