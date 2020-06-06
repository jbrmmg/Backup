package com.jbr.middletier.backup.data;

public class ImportRequest {
    private String path;
    private int source;

    public String getPath() {
        return this.path;
    }

    public int getSource() {
        return this.source;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSource(int source) {
        this.source = source;
    }
}
