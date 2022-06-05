package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;
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
    private static final Logger LOG = LoggerFactory.getLogger(DriveManager.class);

    private final SourceRepository sourceRepository;
    private final ClassificationRepository classificationRepository;

    @Autowired
    public DriveManager(DirectoryRepository directoryRepository,
                        FileRepository fileRepository,
                        SourceRepository sourceRepository,
                        ClassificationRepository classificationRepository,
                        BackupManager backupManager,
                        ActionManager actionManager) {
        super(directoryRepository,fileRepository,backupManager,actionManager);
        this.sourceRepository = sourceRepository;
        this.classificationRepository = classificationRepository;
    }

    private void setSourceStatus(Source source, String status) {
        try {
            source.setStatus(status);
            sourceRepository.save(source);
        } catch(Exception ex) {
            LOG.warn("Failed to set source status.",ex);
        }
    }

    private void processSource(Source nextSource, List<ActionConfirm> deleteActions, Iterable<Classification> classifications) throws IOException {
        if(nextSource.getStatus() != null && nextSource.getStatus().equals("GATHERING")) {
            return;
        }

        if(nextSource.getClass() != Source.class) {
            return;
        }

        setSourceStatus(nextSource,"GATHERING");

        backupManager.postWebLog(BackupManager.webLogLevel.INFO, "Gather - " + nextSource.getPath());

        // If the source does not exist, create it.
        createDirectory(nextSource.getPath());

        try {
            updateDatabase(nextSource, deleteActions, classifications, false);
        } catch (IOException e) {
            setSourceStatus(nextSource,"ERROR");
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to gather + " + e.toString());
            throw e;
        }

        setSourceStatus(nextSource,"OK");
    }

    public void gather() throws IOException {
        Iterable<Source> sources = sourceRepository.findAll();
        Iterable<Classification> classifications = classificationRepository.findAll();

        // Are any files to be deleted?
        List<ActionConfirm> deleteActions = actionManager.findConfirmedDeletes();

        for(Source nextSource: sources) {
            processSource(nextSource,deleteActions,classifications);
        }
    }

    @Override
    void newFileInserted(FileInfo newFile) {
        // Do nothing extra for the file creation.
    }
}
