package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.MetaData;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class FileInfoExtra {
    private final FileDTO file;
    private final MetaDataDTO metaData;
    private final List<FileDTO> backups;
    private final List<String> labels;

    public FileInfoExtra(FileInfo file, MetaData metaData, String fullFilename, String path, String location) {
        this.file = new FileDTO(file,fullFilename,path,location);
        this.metaData = metaData == null ? null : new MetaDataDTO(metaData);
        this.backups = new ArrayList<>();
        this.labels = new ArrayList<>();
    }

    public void addFile(FileInfo file, String fullFilename, String path, String location) { this.backups.add(new FileDTO(file,fullFilename,path,location)); }

    public FileDTO getFile() { return this.file; }

    public List<FileDTO> getBackups() { return this.backups; }

    public void addLabel(String label) {
        this.labels.add(label);
    }

    public List<String> getLabels() { return this.labels; }

    public MetaDataDTO getMetaData() {
        return metaData;
    }
}
