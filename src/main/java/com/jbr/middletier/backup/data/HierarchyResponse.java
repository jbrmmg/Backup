package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HierarchyResponse {
    @Getter
    @Setter
    private int id;
    @Getter
    @Setter
    private String displayName;
    private String name;
    @Setter
    private boolean directory;
    @Setter
    private boolean backup;
    @Getter
    @Setter
    private int underlyingId;
    @Setter
    @Getter
    private LocalDateTime dateTime;
    @Getter
    private String md5;
    @Setter
    @Getter
    private long size;

    public HierarchyResponse() {
        this.id = -1;
        this.name = "/";
        this.displayName = "";
        this.directory = true;
        this.backup = false;
        this.underlyingId = -1;
    }

    public void setPath(String path) { this.name = path; }

    public String getPath() { return this.name; }

    public void setMd5(MD5 md5) {
        this.md5 = md5 != null ? md5.toString() : null;
    }

    public boolean getDirectory() { return this.directory; }

    public boolean getBackup() { return this.backup; }

    public int getOrderingIndex() {
        int result = 0;
        if(this.directory && this.displayName.trim().isEmpty())
            return result;

        List<String> orderValues = Arrays.asList("January","February","March","April","May","June","July","August","September","October","November","December");
        for(String nextOrderValue : orderValues) {
            result++;
            if(this.directory && this.displayName.trim().equalsIgnoreCase(nextOrderValue)) {
                return result;
            }
        }

        result++;
        if(this.directory)
            return result;

        result++;
        return result;
    }

    public int getNumericValue() {
        int result = 0;

        try {
            if(this.directory) {
                result = Integer.parseInt(this.displayName.trim());
            }
        } catch (NumberFormatException e) {
            // Ignore this error.
        }

        return result * -1;
    }

    public String getCompareName() {
        return this.displayName.trim().toLowerCase(Locale.ROOT);
    }
}
