package com.jbr.middletier.backup.data;

public class ConfirmActionRequest {
    private int id;
    private String parameter;

    public int getId() {
        return this.id;
    }

    public String getParameter() {
        return this.parameter;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
}
