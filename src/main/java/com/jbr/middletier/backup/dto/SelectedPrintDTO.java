package com.jbr.middletier.backup.dto;

public class SelectedPrintDTO {
    private int fileId;
    private String sizeName;
    private Boolean border;
    private Boolean blackWhite;

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getSizeName() {
        return sizeName;
    }

    public void setSizeName(String sizeName) {
        this.sizeName = sizeName;
    }

    public Boolean getBorder() {
        return border;
    }

    public void setBorder(Boolean border) {
        this.border = border;
    }

    public Boolean getBlackWhite() {
        return blackWhite;
    }

    public void setBlackWhite(Boolean blackWhite) {
        this.blackWhite = blackWhite;
    }
}
