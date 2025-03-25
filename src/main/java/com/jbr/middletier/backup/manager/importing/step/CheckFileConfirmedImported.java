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

import java.util.Objects;

@Component
public class CheckFileConfirmedImported extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(CheckFileConfirmedImported.class);

    @Autowired
    protected CheckFileConfirmedImported(ImportFileRepository importFileRepository,
                                         AssociatedFileDataManager associatedFileDataManager) {
        super(importFileRepository, associatedFileDataManager);
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED;
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        LOG.info("Checking if file has been imported");

        // If it's been imported then there will be a similar file with the same name, md5, date/time and size.
        for(ImportFileBaseDTO next: file.getSimilarFiles()) {
            if(next.getType() == FileSystemObjectType.FSO_FILE &&
                next.getMd5().equals(file.getMd5()) &&
                next.getFilename().equals(file.getFilename()) &&
                Objects.equals(next.getSize(), file.getSize()) &&
                next.getDate().equals(file.getDate())) {
                return TrafficLightType.TL_GREEN;
            }
        }

        return TrafficLightType.TL_RED;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile record) {
        return false;
    }
}
