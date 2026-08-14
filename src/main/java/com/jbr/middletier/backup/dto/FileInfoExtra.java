package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.CustomMetaData;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.MetaData;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FileInfoExtra {
    private final FileDTO file;
    private final MetaDataDTO metaData;
    private final List<FileDTO> backups;
    private final List<String> labels;

    public FileInfoExtra(FileInfo file, MetaData metaData, CustomMetaData customMetaData, String fullFilename, String path, String location) {
        this.file = new FileDTO(file,fullFilename,path,location);
        this.metaData = metaData == null ? null : new MetaDataDTO(metaData, customMetaData);
        this.backups = new ArrayList<>();
        this.labels = new ArrayList<>();
    }

    public void addFile(FileInfo file, String fullFilename, String path, String location) { this.backups.add(new FileDTO(file,fullFilename,path,location)); }

    public void addLabel(String label) {
        this.labels.add(label);
    }
}
