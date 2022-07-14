package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileInfo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class FileInfoExtra {
    private final FileInfoDTO file;
    private final List<FileInfoDTO> backups;

    public FileInfoExtra(FileInfo file) {
        this.file = new FileInfoDTO(file);
        this.backups = new ArrayList<>();
    }

    public void addFile(FileInfo file) { this.backups.add(new FileInfoDTO(file)); }

    public FileInfoDTO getFile() { return this.file; }

    public List<FileInfoDTO> getBackups() { return this.backups; }
}
