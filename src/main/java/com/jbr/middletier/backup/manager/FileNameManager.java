package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.dataaccess.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class FileNameManager {
    private final FileSystemObjectManager fileSystemObjectManager;
    private final SourceRepository sourceRepository;

    @Autowired
    public FileNameManager(FileSystemObjectManager fileSystemObjectManager,
                           SourceRepository sourceRepository) {
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.sourceRepository = sourceRepository;
    }

    private void populateFileNamePartsList(FileSystemObject fso, List<FileSystemObject> fileNameParts) {
        if(fso.getParentId() == null) {
            return;
        }

        switch(fso.getParentId().getType()) {
            case FSO_FILE:
            case FSO_DIRECTORY:
                fileNameParts.add(fileSystemObjectManager.findFileSystemObject(fso.getParentId()).get());
                break;

            case FSO_SOURCE:
                fileNameParts.add(this.sourceRepository.findById(fso.getParentId().getId()).get());
                break;
        }
    }

    private File getFileNameFromParts(List<FileSystemObject> nameParts) {
        StringBuilder sb = new StringBuilder();
        for(FileSystemObject nextFso : nameParts) {
            sb.append(nextFso.getName());
        }
    }

    public File getFullFileName(FileSystemObject fso) {
        List<FileSystemObject> fileNameParts = new ArrayList<>();
        fileNameParts.add(fso);

        populateFileNamePartsList(fso, fileNameParts);

        Collections.reverse(fileNameParts);
        return getFileNameFromParts(fileNameParts);
    }
}
