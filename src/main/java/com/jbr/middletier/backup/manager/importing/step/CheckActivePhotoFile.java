package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.ImportFileBaseDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CheckActivePhotoFile extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(CheckActivePhotoFile.class);

    private final FileRepository fileRepository;

    @Autowired
    public CheckActivePhotoFile(ImportFileRepository importFileRepository,
                                   FileRepository fileRepository,
                                   ImportSourceManager importSourceManager) {
        super(importFileRepository, importSourceManager);
        this.fileRepository = fileRepository;
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_CHECK_ACTIVE_PHOTO_FILE;
    }

    private ImportFileBaseDTO getSimilarFile(ImportFile importFile) {
        ImportFileBaseDTO similar = new ImportFileBaseDTO();
        similar.setType(FileSystemObjectType.FSO_IMPORT_FILE);
        similar.setFilename(importFile.getName() + " [" + importFile.getIdAndType().getType() + "]");
        similar.setDate(importFile.getImportDate());
        similar.setSize(importFile.getImportSize());
        similar.setMd5(importFile.getImportMd5().isPresent() ? importFile.getImportMd5().get() : null);

        return similar;
    }

    private ImportFileBaseDTO getSimilarFileExisting(FileInfo existingFile) {
        ImportFileBaseDTO similar = new ImportFileBaseDTO();
        similar.setType(FileSystemObjectType.FSO_IMPORT_FILE);
        similar.setFilename(existingFile.getName());
        similar.setDate(existingFile.getDate());
        similar.setSize(existingFile.getSize());
        similar.setMd5(existingFile.getMd5().isPresent() ? existingFile.getMd5().get() : null);

        return similar;
    }

    private boolean isCorrectTypeForActivePhoto(PreImportFileDTO file) {
        // Must be a video.
        if(!file.isVideo()) {
            return false;
        }

        if(file.getDuration() > 4) {
            return false;
        }

        return file.getFilename().toLowerCase().endsWith(".mov") && file.getImportName().toLowerCase().endsWith(".mp4");
    }

    private String getNameWithNoExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    private boolean timeIsClose(FileInfo existingFile, PreImportFileDTO file) {
        // Are they the same date?
        if (!existingFile.getDate().toLocalDate().equals(file.getImportDate().toLocalDate())) {
            return false;
        }

        // Are they within 5 seconds?
        long seconds = Duration.between(existingFile.getDate(), file.getImportDate()).toSeconds();

        return Math.abs(seconds) < 5;
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        LOG.info("Checking active photo file");
        // This check only applies to a file that ends MOV and the import is mp4.
        if(!isCorrectTypeForActivePhoto(file)) {
            return TrafficLightType.TL_GREEN;
        }

        // Active photo is a MOV/mp4 that has the same name as an image in the import.
        for(ImportFile importFile : importFileRepository.findAll()) {
            if(importFile.getName().equalsIgnoreCase(file.getFilename())) {
                continue;
            }

            // Does this match on the name.
            String importFilename = getNameWithNoExtension(importFile.getName());
            String fileFilename = getNameWithNoExtension(file.getFilename());

            if(importFilename.equalsIgnoreCase(fileFilename)) {
                // Add this as a similar file.
                file.addSimilarFile(getSimilarFile(importFile));
                return TrafficLightType.TL_RED;
            }
        }

        // See if there is a file already imported that matches and is close to the date.
        String imageName = getNameWithNoExtension(file.getFilename()) + ".JPEG";
        for(FileInfo existingFile : fileRepository.findByName(imageName)) {
            // Is this file within a few seconds of the import file?
            if(timeIsClose(existingFile, file)) {
                file.addSimilarFile(getSimilarFileExisting(existingFile));
                return TrafficLightType.TL_RED;
            }
        }

        return TrafficLightType.TL_GREEN;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile dbRecord) {
        // This does not get stored.
        return false;
    }
}
