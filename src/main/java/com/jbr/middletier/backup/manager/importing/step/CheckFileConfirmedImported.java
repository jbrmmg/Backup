package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.ImportFileBaseDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class CheckFileConfirmedImported extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(CheckFileConfirmedImported.class);

    @Autowired
    public CheckFileConfirmedImported(ImportFileRepository importFileRepository,
                                         ImportSourceManager importSourceManager) {
        super(importFileRepository, importSourceManager);
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_CHECK_FILE_CONFIRMED_IMPORTED;
    }

    private boolean checkMD5(ImportFileBaseDTO next, PreImportFileDTO file) {
        if(next.getMd5() == null){
            return false;
        }

        if(file.getImportMd5() == null){
            return false;
        }

        return next.getMd5().equals(file.getImportMd5());
    }

    private boolean checkFilename(ImportFileBaseDTO next, PreImportFileDTO file) {
        if(next.getFilename() == null || next.getFilename().isEmpty()){
            return false;
        }

        if(file.getImportName() == null || file.getImportName().isEmpty()){
            return false;
        }

        return next.getFilename().toLowerCase().endsWith(file.getImportName().toLowerCase());
    }

    private boolean checkSize(ImportFileBaseDTO next, PreImportFileDTO file) {
        if(next.getSize() == null){
            return false;
        }

        if(file.getImportSize() == null){
            return false;
        }

        return Objects.equals(next.getSize(), file.getImportSize());
    }

    private boolean checkDate(ImportFileBaseDTO next, PreImportFileDTO file) {
        if(next.getDate() == null){
            return false;
        }

        if(file.getImportDate() == null){
            return false;
        }

        return next.getDate().equals(file.getImportDate());
    }

    private boolean checkByOriginalCustomFields(ImportFileBaseDTO next, PreImportFileDTO file) {
        if(next.getOriginalMd5() == null || file.getMd5() == null) return false;
        if(!next.getOriginalMd5().equals(file.getMd5())) return false;
        if(!Objects.equals(next.getOriginalSize(), file.getSize())) return false;
        if(next.getOriginalFile() == null || file.getFilename() == null) return false;
        if(!next.getOriginalFile().equalsIgnoreCase(file.getFilename())) return false;
        if(next.getDate() == null || file.getImportDate() == null) return false;
        return next.getDate().equals(file.getImportDate());
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        LOG.info("Checking if file has been imported {}", file.getImportName());

        // If it's been imported then there will be a similar file with the same name, md5, date/time and size.
        // A converted MOV re-import is detected via the stored original custom fields.
        int count = 0;
        for(ImportFileBaseDTO next: file.getSimilarFiles()) {
            LOG.debug("Similar check {} {}", next.getFilename(), file.getImportName());
            LOG.debug("Similar check date 1 {}", next.getDate());
            LOG.debug("Similar check date 2 {}", file.getImportDate());
            if(next.getType() == FileSystemObjectType.FSO_FILE &&
                    (checkMD5(next, file) && checkFilename(next, file) && checkSize(next, file) && checkDate(next, file)
                     || checkByOriginalCustomFields(next, file))) {
                count++;
            }
        }

        if(count == 1) {
            return TrafficLightType.TL_GREEN;
        }

        // If the file is in the post import directory then it is potentially imported.
        if(file.isInPostImport()) {
            return TrafficLightType.TL_AMBER;
        }

        return TrafficLightType.TL_RED;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile dbRecord) {
        return false;
    }
}
