package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Component
public class DriveManager extends FileProcessor {
    final static private Logger LOG = LoggerFactory.getLogger(DriveManager.class);

    private final SourceRepository sourceRepository;
    private final ClassificationRepository classificationRepository;
    private final ActionConfirmRepository actionConfirmRepository;

    @Autowired
    public DriveManager(DirectoryRepository directoryRepository,
                        FileRepository fileRepository,
                        SourceRepository sourceRepository,
                        ClassificationRepository classificationRepository,
                        BackupManager backupManager,
                        ActionConfirmRepository actionConfirmRepository,
                        ActionManager actionManager) {
        super(directoryRepository,fileRepository,backupManager,actionManager);
        this.sourceRepository = sourceRepository;
        this.classificationRepository = classificationRepository;
        this.actionConfirmRepository = actionConfirmRepository;
    }

    private void setSourceStatus(Source source, String status) {
        try {
            source.setStatus(status);
            sourceRepository.save(source);
        } catch(Exception ex) {
            LOG.warn("Failed to set source status.",ex);
        }
    }

    public void gather() throws Exception {
        Iterable<Source> sources = sourceRepository.findAll();
        Iterable<Classification> classifications = classificationRepository.findAll();

        // Are any files to be deleted?
        List<ActionConfirm> deleteActions = actionConfirmRepository.findByConfirmedAndAction(true,"DELETE");

        for(Source nextSource: sources) {
            if(nextSource.getStatus() != null && nextSource.getStatus().equals("GATHERING")) {
                continue;
            }

            if(nextSource.getTypeEnum() != Source.SourceTypeType.Standard ) {
                continue;
            }

            fileRepository.markAllRemoved(nextSource.getId());
            directoryRepository.markAllRemoved(nextSource.getId());

            setSourceStatus(nextSource,"GATHERING");

            backupManager.postWebLog(BackupManager.webLogLevel.INFO, "Gather - " + nextSource.getPath());

            // If the source does not exist, create it.
            createDirectory(nextSource.getPath());

            try(Stream<Path> paths = Files.walk(Paths.get(nextSource.getPath()))) {
                // Read directory structure into the database.
                paths
                   .forEach(path -> processPath(path,deleteActions,nextSource,classifications,false));
            } catch (IOException e) {
                setSourceStatus(nextSource,"ERROR");
                backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to gather + " + e.toString());
                throw e;
            }

            setSourceStatus(nextSource,"OK");
        }

        fileRepository.deleteRemoved();
        directoryRepository.deleteRemoved();
    }

    @Override
    void newFileInserted(FileInfo newFile) {
        // Do nothing extra for the file creation.
    }
}
