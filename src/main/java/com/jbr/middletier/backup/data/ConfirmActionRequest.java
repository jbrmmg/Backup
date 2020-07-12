package com.jbr.middletier.backup.data;

public class ConfirmActionRequest {
    private int id;
    private String parameter;
    private Boolean confirm;

    public int getId() {
        return this.id;
    }

    public String getParameter() {
        return this.parameter;
    }

    public Boolean getConfirm() { return this.confirm; }

    public void setId(int id) {
        this.id = id;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public void setConfirm(Boolean confirm) { this.confirm = confirm; }
}
