package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
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

    @Autowired
    public DriveManager(DirectoryRepository directoryRepository,
                        FileRepository fileRepository,
                        AssociatedFileDataManager associatedFileDataManager,
                        BackupManager backupManager,
                        ActionManager actionManager) {
        super(directoryRepository,fileRepository,backupManager,actionManager,associatedFileDataManager);
    }

    private void processSource(Source nextSource, List<ActionConfirm> deleteActions) throws IOException {
        if(nextSource.getStatus() != null && SourceStatusType.SST_GATHERING.equals(nextSource.getStatus())) {
            return;
        }

        if(nextSource.getClass() != Source.class) {
            return;
        }

        associatedFileDataManager.updateSourceStatus(nextSource,SourceStatusType.SST_GATHERING);
        backupManager.postWebLog(BackupManager.webLogLevel.INFO, "Gather - " + nextSource.getPath());

        // If the source does not exist, create it.
        createDirectory(nextSource.getPath());

        try {
            updateDatabase(nextSource, deleteActions, false);
        } catch (IOException e) {
            associatedFileDataManager.updateSourceStatus(nextSource,SourceStatusType.SST_ERROR);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to gather + " + e);
            throw e;
        }

        associatedFileDataManager.updateSourceStatus(nextSource,SourceStatusType.SST_OK);
    }

    public void gather() throws IOException {
        // Are any files to be deleted?
        List<ActionConfirm> deleteActions = actionManager.findConfirmedDeletes();

        for(Source nextSource: associatedFileDataManager.internalFindAllSource()) {
            processSource(nextSource, deleteActions);
        }
    }

    @Override
    void newFileInserted(FileInfo newFile) {
        // Do nothing extra for the file creation.
    }
}
