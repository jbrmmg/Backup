package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.jbr.middletier.backup.manager.FileProcessor;
import java.io.File;

@Component
public class ReadPreImportFile extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(ReadPreImportFile.class);

    protected final FileSystem fileSystem;

    @Autowired
    public ReadPreImportFile(FileSystem fileSystem,
                             ImportFileRepository importFileRepository,
                             AssociatedFileDataManager associatedFileDataManager) {
        super(importFileRepository, associatedFileDataManager);
        this.fileSystem = fileSystem;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile record) {
        // Transfer the data from the file to the record.
        if(record.getSize() == null) {
            record.setSize(file.getSize());
            record.setDate(file.getDate());
            record.setMD5(new MD5(file.getMd5()));

            return true;
        }

        return false;
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_READ_PREIMPORT_FILE;
    }

    protected boolean getMD5(PreImportFileDTO file, File realWorldFile, boolean importMd5) {
        try {
            Classification dummyClassification = new Classification();
            dummyClassification.setUseMD5(true);
            MD5 md5 = fileSystem.getClassifiedFileMD5(realWorldFile.toPath(), dummyClassification, 0);
            if(importMd5) {
                file.setImportMd5(md5.toString());
            } else {
                file.setMd5(md5);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        LOG.info("Read Pre Import file data {}", file.getFilename());

        // If the data is already in the database then there is no need to read again.
        if(file.isInDatabase() &&
            file.getSize() != null &&
            file.getDate() != null ) {
            if(file.getMd5() != null) {
                return TrafficLightType.TL_GREEN;
            }

            return TrafficLightType.TL_AMBER;
        }

        // Read the pre import file data.
        File preImportFile = getPreImportFilename(file);

        if(preImportFile.exists()) {
            // Read the size, date and MD5
            file.setSize(preImportFile.length());
            file.setDate(FileProcessor.getFileLastModified(preImportFile));

            if (getMD5(file, preImportFile, false)) {
                saveData(file);
                return TrafficLightType.TL_GREEN;
            }

            saveData(file);
            LOG.warn("Cannot determine MD5 of {}", file.getFilename());
            return TrafficLightType.TL_AMBER;
        }

        return TrafficLightType.TL_RED;
    }
}
