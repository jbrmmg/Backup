package com.jbr.middletier.backup.data;

public class HierarchyResponse {
    private int id;
    private int level;
    private String displayName;
    private String path;
    private boolean directory;
    private int underlyingId;

    public HierarchyResponse() {
        this.id = -1;
        this.level = 0;
        this.path = "/";
        this.displayName = "";
        this.directory = true;
        this.underlyingId = -1;
    }

    public HierarchyResponse (int id, int level, String path, int underlyingId) {
        this.id = id;
        this.level = level;
        this.path = path;
        this.displayName = "";
        this.directory = true;
        this.underlyingId = underlyingId;
    }

    public void setId(int id) { this.id = id; }

    public int getId() { return this.id; }

    public void setLevel(int level) { this.level = level; }

    public int getLevel() { return this.level; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return this.displayName; }

    public void setPath(String path) { this.path = path; }

    public String getPath() { return this.path; }

    public void setDirectory(boolean directory) { this.directory = directory; }

    public boolean getDirectory() { return this.directory; }

    public void setUnderlyingId(int underlyingId) { this.underlyingId = underlyingId; }

    public int getUnderlyingId() { return this.underlyingId; }
}
