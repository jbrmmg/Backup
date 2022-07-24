package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.ActionConfirm;

@SuppressWarnings("unused")
public class ActionConfirmDTO {
    private int id;
    private int fileId;
    private String fileName;
    private String action;
    private Boolean confirmed;
    private Boolean parameterRequired;
    private String parameter;
    private String flags;
    private boolean isImage;
    private boolean isVideo;

    public int getId() {
        return id;
    }

    public int getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getAction() {
        return action;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public Boolean getParameterRequired() {
        return parameterRequired;
    }

    public String getParameter() {
        return parameter;
    }

    public String getFlags() {
        return flags;
    }

    public boolean getIsImage() {
        return isImage;
    }

    public boolean getIsVideo() {
        return isVideo;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public void setParameterRequired(Boolean parameterRequired) {
        this.parameterRequired = parameterRequired;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public void setIsImage(boolean image) {
        isImage = image;
    }

    public void setIsVideo(boolean video) {
        isVideo = video;
    }
}
