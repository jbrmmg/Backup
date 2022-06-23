package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.DuplicateDataDTO;
import com.jbr.middletier.backup.exception.MissingFileSystemObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DuplicateManager {
    private static final Logger LOG = LoggerFactory.getLogger(DuplicateManager.class);

    private final AssociatedFileDataManager associatedFileDataManager;
    private final FileSystemObjectManager fileSystemObjectManager;
    private final ActionManager actionManager;

    @Autowired
    public DuplicateManager(AssociatedFileDataManager associatedFileDataManager,
                            FileSystemObjectManager fileSystemObjectManager,
                            ActionManager actionManager) {
        this.associatedFileDataManager = associatedFileDataManager;
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.actionManager = actionManager;
    }

    private void processDuplicate(FileInfo potentialDuplicate, DuplicateDataDTO data) throws MissingFileSystemObject {
        // TODO - test this method.
        if(this.actionManager.checkAction(potentialDuplicate, ActionConfirmType.AC_DELETE_DUPLICATE)) {
            LOG.info("Delete duplicate file - {}", potentialDuplicate);

            File fileToDelete = fileSystemObjectManager.getFile(potentialDuplicate);
            if(fileToDelete.exists()) {
                LOG.info("Delete.");
                try {
                    Files.delete(fileToDelete.toPath());
                    data.increment(DuplicateDataDTO.DuplicateCountType.DELETED);
                } catch (IOException e) {
                    LOG.warn("Failed to delete {}",fileToDelete);
                }
            }
        }
    }

    private void checkDuplicateOfFile(Iterable<FileInfo> files, DuplicateDataDTO data) throws MissingFileSystemObject {
        // If files have the same size and MD5 then they are potential duplicates.
        for(FileInfo nextFile: files) {
            for(FileInfo nextFile2: files) {
                if(nextFile.duplicate(nextFile2)) {
                    LOG.info("Duplicate - {}", nextFile);

                    processDuplicate(nextFile,data);
                    processDuplicate(nextFile2,data);
                }
            }
        }
    }

    private void checkDuplicatesOnSource(Source source, DuplicateDataDTO data) {
        try {
            List<DirectoryInfo> directories = new ArrayList<>();
            List<FileInfo> files = new ArrayList<>();

            fileSystemObjectManager.loadByParent(source.getIdAndType().getId(), directories, files);

            Map<String, Long> nameCount = files
                    .stream()
                    .collect(Collectors.groupingBy(FileInfo::getName, Collectors.counting()));

            for (Map.Entry<String, Long> nextEntry : nameCount.entrySet()) {
                if (nextEntry.getValue() > 1) {
                    // Check this name for being a duplicate.
                    checkDuplicateOfFile(files
                            .stream()
                            .filter(file -> file.getName().equals(nextEntry.getKey()))
                            .collect(Collectors.toList()),
                            data);
                    data.increment(DuplicateDataDTO.DuplicateCountType.CHECKED);
                }
            }
        } catch (MissingFileSystemObject e) {
            data.setProblems();
        }
    }

    public List<DuplicateDataDTO> duplicateCheck() {
        List<DuplicateDataDTO> result = new ArrayList<>();

        actionManager.clearDuplicateActions();

        // Check for duplicates in sources.
        for (Source nextSource : associatedFileDataManager.internalFindAllSource()) {
            if (Boolean.TRUE.equals(nextSource.getLocation().getCheckDuplicates())) {
                DuplicateDataDTO duplicateDataDTO = new DuplicateDataDTO(nextSource.getIdAndType().getId());
                result.add(duplicateDataDTO);
                checkDuplicatesOnSource(nextSource, duplicateDataDTO);
            }
        }

        return result;
    }
}
