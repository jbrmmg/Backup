package com.jbr.middletier.backup.data;

import java.util.ArrayList;
import java.util.List;

public class FileInfoExtra {
    private FileInfo file;
    private List<FileInfo> backups;

    public FileInfoExtra(FileInfo file) {
        this.file = file;
        this.backups = new ArrayList<>();
    }

    public FileInfo getFile() { return this.file; }

    public List<FileInfo> getBackups() { return this.backups; }

    public void setBackups(List<FileInfo> backups) { this.backups = backups; }
}
