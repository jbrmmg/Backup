package com.jbr.middletier.backup.data;

public class OkStatus {
    private String status;

    public OkStatus() {
        status = "OK";
    }

    public String getStatus() { return this.status; }

    public OkStatus setStatus(String status) { this.status = status; return this; }

    public static OkStatus getOkStatus() {
        return new OkStatus();
    }
}
