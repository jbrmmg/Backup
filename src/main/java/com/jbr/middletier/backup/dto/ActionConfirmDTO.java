package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.ActionConfirm;
import com.jbr.middletier.backup.data.ActionConfirmType;

public class ActionConfirmDTO {
    private final int id;
    private final int fileId;
    private final String fileName;
    private final ActionConfirmType action;
    private final Boolean confirmed;
    private final Boolean parameterRequired;
    private final String parameter;
    private final String flags;

    public ActionConfirmDTO(ActionConfirm data) {
        this.id = data.getId();
        this.action = data.getAction();
        this.fileId = data.getPath().getIdAndType().getId();
        this.fileName = data.getPath().getName();
        this.parameterRequired = data.getParameterRequired();
        this.parameter = data.getParameter();
        this.flags = data.getFlags();
        this.confirmed = data.confirmed();
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

    public ActionConfirmType getAction() {
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
}
