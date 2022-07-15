package com.jbr.middletier.backup.data;

@SuppressWarnings("unused")
public class HierarchyResponse {
    private int id;
    private String displayName;
    private String name;
    private boolean directory;
    private boolean backup;
    private int underlyingId;

    public HierarchyResponse() {
        this.id = -1;
        this.name = "/";
        this.displayName = "";
        this.directory = true;
        this.backup = false;
        this.underlyingId = -1;
    }

    public void setId(int id) { this.id = id; }

    public int getId() { return this.id; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return this.displayName; }

    public void setPath(String path) { this.name = path; }

    public String getPath() { return this.name; }

    public void setDirectory(boolean directory) { this.directory = directory; }

    public boolean getDirectory() { return this.directory; }

    public void setUnderlyingId(int underlyingId) { this.underlyingId = underlyingId; }

    public int getUnderlyingId() { return this.underlyingId; }

    public void setBackup(boolean backup) { this.backup = backup; }

    public boolean getBackup() { return this.backup; }
}
