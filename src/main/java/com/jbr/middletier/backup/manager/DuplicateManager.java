package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.ActionConfirmRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Component
public class DuplicateManager {
    private static final Logger LOG = LoggerFactory.getLogger(DuplicateManager.class);

    private final ActionConfirmRepository actionConfirmRepository;
    private final SourceRepository sourceRepository;
    private final FileRepository fileRepository;
    private final ActionManager actionManager;

    @Autowired
    public DuplicateManager(ActionConfirmRepository actionConfirmRepository,
                            SourceRepository sourceRepository,
                            FileRepository fileRepository,
                            ActionManager actionManager) {
        this.actionConfirmRepository = actionConfirmRepository;
        this.sourceRepository = sourceRepository;
        this.fileRepository = fileRepository;
        this.actionManager = actionManager;
    }

    private void processDuplicate(FileInfo potentialDuplicate) {
        if(this.actionManager.checkAction(potentialDuplicate,"DELETE_DUP")) {
            LOG.info("Delete duplicate file - {}", potentialDuplicate);

            if(true)
                throw new IllegalStateException("Fix this");
            String deleteFile = "";// = potentialDuplicate.getDirectoryInfo().getSource().getPath() + potentialDuplicate.getDirectoryInfo().getName() + "/" + potentialDuplicate.getName();

            File fileToDelete = new File(deleteFile);
            if(fileToDelete.exists()) {
                LOG.info("Delete.");
                try {
                    Files.delete(fileToDelete.toPath());
                } catch (IOException e) {
                    LOG.warn("Failed to delete {}",fileToDelete);
                }
            }
        }
    }

    private void checkDuplicateOfFile(List<FileInfo> files, Source source) {
        // Get list of files from the original source.
        List<FileInfo> checkFiles = new ArrayList<>();

        for(FileInfo nextFile: files) {
            if(true)
                throw new IllegalStateException("Fix this");
//            if(nextFile.getDirectoryInfo().getSource().getId() == source.getId()) {
                checkFiles.add(nextFile);
//            }
        }

        if(checkFiles.size() <= 1) {
            return;
        }

        // If files have the same size and MD5 then they are potential duplicates.
        for(FileInfo nextFile: checkFiles) {
            for(FileInfo nextFile2: checkFiles) {
                if(nextFile.duplicate(nextFile2)) {
                    LOG.info("Duplicate - {}", nextFile);

                    processDuplicate(nextFile);
                    processDuplicate(nextFile2);
                }
            }
        }
    }

    public void duplicateCheck() {
        actionConfirmRepository.clearDuplicateDelete(false);

        Iterable<Source> sources = sourceRepository.findAll();

        // Check for duplicates in sources.
        for(Source nextSource: sources) {
            if(Boolean.TRUE.equals(nextSource.getLocation().getCheckDuplicates())) {
                // Find files that have the same name and size.
                if(true)
                    throw new IllegalStateException("fix this");
                List<String> files = new ArrayList<>();//fileRepository.findDuplicates(nextSource.getId());

                // Are these files really duplicates.
                for(String nextFile: files) {
                    List<FileInfo> duplicates = fileRepository.findByName(nextFile);
                    LOG.info("Check: {}", nextFile);
                    checkDuplicateOfFile(duplicates, nextSource);
                }
            }
        }
    }
}
