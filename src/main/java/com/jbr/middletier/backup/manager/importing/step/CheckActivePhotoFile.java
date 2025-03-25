package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.ImportFileBaseDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CheckActivePhotoFile extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(CheckActivePhotoFile.class);

    @Autowired
    protected CheckActivePhotoFile(ImportFileRepository importFileRepository,
                                   AssociatedFileDataManager associatedFileDataManager) {
        super(importFileRepository, associatedFileDataManager);
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_CHECK_ACTIVE_PHOTO_FILE;
    }

    private ImportFileBaseDTO getSimilarFile(ImportFile importFile) {
        ImportFileBaseDTO similar = new ImportFileBaseDTO();
        similar.setType(FileSystemObjectType.FSO_IMPORT_FILE);
        similar.setFilename(importFile.getName() + " [" + importFile.getIdAndType().getType() + "]");
        similar.setDate(importFile.getDate());
        similar.setSize(importFile.getSize());
        similar.setMd5(importFile.getMD5());

        return similar;
    }

    private boolean isWriteTypeForActivePhoto(PreImportFileDTO file) {
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

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        LOG.info("Checking active photo file");
        // This check only applies to a file that ends MOV and the import is mp4.
        if(!isWriteTypeForActivePhoto(file)) {
            return TrafficLightType.TL_GREEN;
        }

        // Active photo is a MOV/mp4 that has the same name and the mp4 will have a close date.
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

        return TrafficLightType.TL_GREEN;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile record) {
        // This does not get stored.
        return false;
    }
}
