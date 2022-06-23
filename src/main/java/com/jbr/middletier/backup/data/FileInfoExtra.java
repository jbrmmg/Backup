package com.jbr.middletier.backup.data;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class FileInfoExtra {
    private final FileInfo file;
    private List<FileInfo> backups;

    public FileInfoExtra(FileInfo file) {
        // TODO - this should create FileInfoDTO as its returned externally.
        this.file = file;
        this.backups = new ArrayList<>();
    }

    public FileInfo getFile() { return this.file; }

    public List<FileInfo> getBackups() { return this.backups; }

    public void setBackups(List<FileInfo> backups) { this.backups = backups; }
}
