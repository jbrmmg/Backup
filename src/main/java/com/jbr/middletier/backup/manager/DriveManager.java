package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
public class DriveManager extends FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(DriveManager.class);

    @Autowired
    public DriveManager(AssociatedFileDataManager associatedFileDataManager,
                        BackupManager backupManager,
                        ActionManager actionManager,
                        FileSystemObjectManager fileSystemObjectManager,
                        FileSystem fileSystem) {
        super(backupManager, actionManager, associatedFileDataManager, fileSystemObjectManager, fileSystem);
    }

    private void processSource(Source nextSource, List<ActionConfirm> deleteActions, List<GatherDataDTO> data) {
        if(nextSource.getStatus() != null && SourceStatusType.SST_GATHERING.equals(nextSource.getStatus())) {
            return;
        }

        associatedFileDataManager.updateSourceStatus(nextSource,SourceStatusType.SST_GATHERING);
        backupManager.postWebLog(BackupManager.webLogLevel.INFO, "Gather - " + nextSource.getPath());

        GatherDataDTO gatherData = new GatherDataDTO(nextSource.getIdAndType().getId());

        try {
            // If the source does not exist, create it.
            fileSystem.createDirectory(new File(nextSource.getPath()).toPath());

            updateDatabase(nextSource, deleteActions, false, gatherData);

            associatedFileDataManager.updateSourceStatus(nextSource,SourceStatusType.SST_OK);
        } catch (IOException e) {
            associatedFileDataManager.updateSourceStatus(nextSource, SourceStatusType.SST_ERROR);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR, "Failed to gather " + e);
            gatherData.setProblems();
        }

        data.add(gatherData);
    }

    public List<GatherDataDTO> gather() {
        List<GatherDataDTO> result = new ArrayList<>();

        // Are any files to be deleted?
        List<ActionConfirm> deleteActions = actionManager.findConfirmedDeletes();

        for(Source nextSource: associatedFileDataManager.internalFindAllSource()) {
            if(nextSource.getIdAndType().getType() == FileSystemObjectType.FSO_SOURCE) {
                LOG.info("Process Source {}", nextSource);
                processSource(nextSource, deleteActions, result);
            }
        }

        return result;
    }

    @Override
    public FileInfo createNewFile() {
        // Create a basic file.
        return new FileInfo();
    }
}
