package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.jbr.middletier.backup.manager.FileProcessor;
import java.io.File;
import java.util.Optional;

@Component
public class ReadPreImportFile extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(ReadPreImportFile.class);

    protected final FileSystem fileSystem;

    @Autowired
    public ReadPreImportFile(FileSystem fileSystem,
                             ImportFileRepository importFileRepository,
                             ImportSourceManager importSourceManager) {
        super(importFileRepository, importSourceManager);
        this.fileSystem = fileSystem;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile dbRecord) {
        // Transfer the data from the file to the record.
        if(dbRecord.getSize() == null) {
            dbRecord.setSize(file.getSize());
            dbRecord.setDate(file.getDate());
            if(file.getMd5Optional().isPresent()) {
                dbRecord.setMd5(file.getMd5Optional().get());
            }

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
            Optional<MD5> md5 = fileSystem.getFileMD5(realWorldFile.toPath(), 0);
            if(md5.isPresent()) {
                if(importMd5) {
                    file.setImportMd5(md5.get());
                } else {
                    file.setMd5(md5.get());
                }
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

        // Read the pre-import file data.
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
