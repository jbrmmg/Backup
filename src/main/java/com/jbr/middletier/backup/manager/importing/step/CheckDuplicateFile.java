package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.ImportFileBaseDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CheckDuplicateFile extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(CheckDuplicateFile.class);

    private final FileRepository fileRepository;
    private final FileSystemObjectManager fileSystemObjectManager;
    private final List<Source> validSources;

    @Autowired
    protected CheckDuplicateFile(ImportFileRepository importFileRepository,
                                 AssociatedFileDataManager associatedFileDataManager,
                                 FileRepository fileRepository,
                                 FileSystemObjectManager fileSystemObjectManager) {
        super(importFileRepository, associatedFileDataManager);
        this.fileRepository = fileRepository;
        this.validSources = new ArrayList<>();
        this.fileSystemObjectManager = fileSystemObjectManager;
    }

    @PostConstruct
    public void init() {
        // Set up the valid sources.
        for(Synchronize synchronize: this.associatedFileDataManager.findAllSynchronize()) {
            if(!validSources.contains(synchronize.getSource())) {
                validSources.add(synchronize.getSource());
            }
        }
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_CHECK_DUPLICATE_FILE;
    }

    private void addSimilarFile(File file, FileInfo dbFile, PreImportFileDTO importFile) {
        // Set up the similar file data.
        ImportFileBaseDTO similar = new ImportFileBaseDTO();
        similar.setType(FileSystemObjectType.FSO_FILE);
        similar.setFilename(file.getPath());
        similar.setSize(dbFile.getSize());
        similar.setMd5(dbFile.getMD5());
        similar.setDate(dbFile.getDate());

        // Make sure this is not in the list already.
        boolean addToList = true;
        for(ImportFileBaseDTO next: importFile.getSimilarFiles()) {
            if(next.getFilename().equals(similar.getFilename())) {
                addToList = false;
                break;
            }
        }

        // If required add the file to the list.
        if(addToList) {
            importFile.addSimilarFile(similar);
        }
    }

    private File validSource(FileInfo dbFile) {
        if(dbFile.getIdAndType().getType().equals(FileSystemObjectType.FSO_IGNORE_FILE) ||
                dbFile.getIdAndType().getType().equals(FileSystemObjectType.FSO_IMPORT_FILE)) {
            return null;
        }

        // Is this file from the right source?
        File file = fileSystemObjectManager.getFile(dbFile);
        if(!file.getPath().equalsIgnoreCase(file.getName())) {
            // Only accept file if its from the valid source.
            AtomicBoolean accept = new AtomicBoolean(false);
            validSources.forEach(source -> {
                if(file.getPath().contains(source.getPath())) {
                    accept.set(true);
                }
            });

            if(!accept.get()) {
                return null;
            }
        }

        // Return file
        return file;
    }

    private void getSimilarByMd5(String md5, PreImportFileDTO importFile) {
        for(FileInfo next : fileRepository.findByMd5(md5)) {
            File file = validSource(next);
            if(file != null) {
                // Add this to the similar file list if it's not already there.
                addSimilarFile(file, next, importFile);
            }
        }
    }

    private void getSimilarByName(String name, PreImportFileDTO importFile) {
        for(FileInfo next : fileRepository.findByName(name)) {
            File file = validSource(next);
            if(file != null) {
                // Add this to the similar file list if it's not already there.
                addSimilarFile(file, next, importFile);
            }
        }
    }

    private void getSimilarByDate(LocalDateTime date, PreImportFileDTO importFile) {
        for(FileInfo next : fileRepository.findByDate(date)) {
            File file = validSource(next);
            if(file != null) {
                // Add this to the similar file list if it's not already there.
                addSimilarFile(file, next, importFile);
            }
        }
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        LOG.info("Checking duplicate file");
        getSimilarByMd5(file.getMd5(), file);
        getSimilarByName(file.getFilename(), file);
        getSimilarByDate(file.getDate(), file);
        if(file.getImportMd5() != null) {
            getSimilarByMd5(file.getMd5(), file);
        }
        if(file.getImportName() != null) {
            getSimilarByName(file.getImportName(), file);
        }
        if(file.getImportDate() != null) {
            getSimilarByDate(file.getImportDate(), file);
        }

        // If there are multiple files with the same MD5 then this file has been duplicated.
        List<String> existingMd5s = new ArrayList<>();
        for(ImportFileBaseDTO next: file.getSimilarFiles()) {
            if(existingMd5s.contains(next.getMd5())) {
                // This looks like there is a duplicate.
                return TrafficLightType.TL_RED;
            }

            existingMd5s.add(next.getMd5());
        }

        // If there are multiple files with the same date/time then this is a bit suspect.
       List<LocalDateTime> existingDateTime = new ArrayList<>();
        for(ImportFileBaseDTO next: file.getSimilarFiles()) {
            if(existingDateTime.contains(next.getDate())) {
                // This looks like there is a duplicate.
                return TrafficLightType.TL_RED;
            }

            existingDateTime.add(next.getDate());
        }

        return TrafficLightType.TL_GREEN;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile record) {
        return false;
    }
}
