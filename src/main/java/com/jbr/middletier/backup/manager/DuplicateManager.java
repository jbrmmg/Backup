package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.DuplicateDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DuplicateManager {
    private static final Logger LOG = LoggerFactory.getLogger(DuplicateManager.class);

    private final AssociatedFileDataManager associatedFileDataManager;
    private final FileSystemObjectManager fileSystemObjectManager;
    private final ActionManager actionManager;
    private final FileSystem fileSystem;

    @Autowired
    public DuplicateManager(AssociatedFileDataManager associatedFileDataManager,
                            FileSystemObjectManager fileSystemObjectManager,
                            ActionManager actionManager,
                            FileSystem fileSystem) {
        this.associatedFileDataManager = associatedFileDataManager;
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.actionManager = actionManager;
        this.fileSystem = fileSystem;
    }

    private void processDuplicate(FileInfo potentialDuplicate, DuplicateDataDTO data) {
        if(this.actionManager.checkAction(potentialDuplicate, ActionConfirmType.AC_DELETE_DUPLICATE)) {
            LOG.info("Delete duplicate file - {}", potentialDuplicate);

            data.increment(DuplicateDataDTO.DuplicateCountType.DELETED);
            File fileToDelete = fileSystemObjectManager.getFile(potentialDuplicate);
            fileSystem.deleteFile(fileToDelete, data);
        }
    }

    private void checkDuplicateOfFile(Iterable<FileInfo> files, DuplicateDataDTO data) {
        // If files have the same size and MD5 then they are potential duplicates.
        for(FileInfo nextFile: files) {
            for(FileInfo nextFile2: files) {
                if(nextFile.duplicate(nextFile2)) {
                    LOG.info("Duplicate - {}", nextFile);

                    processDuplicate(nextFile,data);
                }
            }
        }
    }

    private void checkDuplicatesOnSource(Source source, DuplicateDataDTO data) {
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
