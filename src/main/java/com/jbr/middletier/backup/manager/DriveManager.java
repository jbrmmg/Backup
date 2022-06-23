package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.exception.FileProcessException;
import com.jbr.middletier.backup.exception.MissingFileSystemObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
public class DriveManager extends FileProcessor {
    @Autowired
    public DriveManager(AssociatedFileDataManager associatedFileDataManager,
                        BackupManager backupManager,
                        ActionManager actionManager,
                        FileSystemObjectManager fileSystemObjectManager) {
        super(backupManager,actionManager,associatedFileDataManager, fileSystemObjectManager);
    }

    private void processSource(Source nextSource, List<ActionConfirm> deleteActions, List<GatherDataDTO> data) {
        // TODO - test more of this method
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

        GatherDataDTO gatherData = new GatherDataDTO(nextSource.getIdAndType().getId());

        try {
            updateDatabase(nextSource, deleteActions, false, gatherData);

            associatedFileDataManager.updateSourceStatus(nextSource,SourceStatusType.SST_OK);
        } catch (IOException e) {
            associatedFileDataManager.updateSourceStatus(nextSource,SourceStatusType.SST_ERROR);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to gather " + e);
            gatherData.setProblems();
        } catch (FileProcessException e) {
            associatedFileDataManager.updateSourceStatus(nextSource,SourceStatusType.SST_ERROR);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to update database as part of gather " + e);
            gatherData.setProblems();
        } catch (MissingFileSystemObject e) {
            associatedFileDataManager.updateSourceStatus(nextSource,SourceStatusType.SST_ERROR);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Missing File execption in gather " + e);
            gatherData.setProblems();
        }

        data.add(gatherData);
    }

    public List<GatherDataDTO> gather() {
        List<GatherDataDTO> result = new ArrayList<>();

        // Are any files to be deleted?
        List<ActionConfirm> deleteActions = actionManager.findConfirmedDeletes();

        // TODO - check that this does not process the import source!
        for(Source nextSource: associatedFileDataManager.internalFindAllSource()) {
            processSource(nextSource, deleteActions, result);
        }

        return result;
    }

    @Override
    public FileInfo createNewFile() {
        // Create a basic file.
        return new FileInfo();
    }
}
