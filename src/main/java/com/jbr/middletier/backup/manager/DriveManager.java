package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.dto.GatherDataDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.exception.FileProcessException;
import com.jbr.middletier.backup.filetree.*;
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

    private void processSource(Source nextSource, List<ActionConfirm> deleteActions, List<GatherDataDTO> data) {
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
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to gather + " + e);
            gatherData.setProblems();
        } catch (FileProcessException e) {
            associatedFileDataManager.updateSourceStatus(nextSource,SourceStatusType.SST_ERROR);
            backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to update database as part of gather + " + e);
            gatherData.setProblems();
        }

        data.add(gatherData);
    }

    public List<GatherDataDTO> gather() throws IOException {
        List<GatherDataDTO> result = new ArrayList<>();

        // Are any files to be deleted?
        List<ActionConfirm> deleteActions = actionManager.findConfirmedDeletes();

        for(Source nextSource: associatedFileDataManager.internalFindAllSource()) {
            processSource(nextSource, deleteActions, result);
        }

        return result;
    }

    @Override
    void newFileInserted(FileInfo newFile) {
        // Do nothing extra for the file creation.
    }
}
