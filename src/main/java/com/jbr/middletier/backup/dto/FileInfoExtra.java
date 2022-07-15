package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileInfo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class FileInfoExtra {
    private final FileDTO file;
    private final List<FileDTO> backups;

    public FileInfoExtra(FileInfo file, String fullFilename, String path, String location) {
        this.file = new FileDTO(file,fullFilename,path,location);
        this.backups = new ArrayList<>();
    }

    public void addFile(FileInfo file, String fullFilename, String path, String location) { this.backups.add(new FileDTO(file,fullFilename,path,location)); }

    public FileDTO getFile() { return this.file; }

    public List<FileDTO> getBackups() { return this.backups; }
}
