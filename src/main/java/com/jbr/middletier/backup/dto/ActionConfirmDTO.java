package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.ActionConfirm;
import com.jbr.middletier.backup.data.ActionConfirmType;

@SuppressWarnings("unused")
public class ActionConfirmDTO {
    private final int id;
    private final int fileId;
    private final String fileName;
    private final String action;
    private final Boolean confirmed;
    private final Boolean parameterRequired;
    private final String parameter;
    private final String flags;
    private final boolean isImage;
    private final boolean isVideo;

    public ActionConfirmDTO(ActionConfirm data) {
        this.id = data.getId();
        this.action = data.getAction().getTypeName();
        this.fileId = data.getPath().getIdAndType().getId();
        this.fileName = data.getPath().getName();
        this.parameterRequired = data.getParameterRequired();
        this.parameter = data.getParameter();
        this.flags = data.getFlags();
        this.confirmed = data.confirmed();
        this.isImage = data.getPath().getClassification().getIsImage();
        this.isVideo = data.getPath().getClassification().getIsVideo();
    }

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

    public boolean isImage() {
        return isImage;
    }

    public boolean isVideo() {
        return isVideo;
    }
}
