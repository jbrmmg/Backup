package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.IgnoreFile;
import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.IgnoreFileRepository;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.ImportFileBaseDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CheckFileIgnored extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(CheckFileIgnored.class);

    private final IgnoreFileRepository ignoreFileRepository;

    @Autowired
    protected CheckFileIgnored(ImportFileRepository importFileRepository,
                               ImportSourceManager importSourceManager,
                               AssociatedFileDataManager associatedFileDataManager,
                               IgnoreFileRepository ignoreFileRepository) {
        super(importFileRepository, associatedFileDataManager, importSourceManager);
        LOG.trace("CheckFileIgnored created");
        this.ignoreFileRepository = ignoreFileRepository;
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_CHECK_FILE_IGNORED;
    }

    private ImportFileBaseDTO getSimilarFile(IgnoreFile ignoreFile) {
        ImportFileBaseDTO similar = new ImportFileBaseDTO();
        similar.setType(FileSystemObjectType.FSO_IGNORE_FILE);
        similar.setFilename(ignoreFile.getName() + " [" + ignoreFile.getIdAndType().getType() + "]");
        similar.setDate(ignoreFile.getDate());
        similar.setSize(ignoreFile.getSize());
        similar.setMd5(ignoreFile.getMD5());

        return similar;
    }

    private boolean checkOnMd5(PreImportFileDTO file) {
        for(IgnoreFile nextIgnoreFile : ignoreFileRepository.findAllByOrderByIdAsc()) {
            // Does this match on the file MD5
            if(nextIgnoreFile.getMD5().toString().equals(file.getMd5()) && nextIgnoreFile.getSize().equals(file.getSize())) {
                file.addSimilarFile(getSimilarFile(nextIgnoreFile));
                return true;
            }
            if(nextIgnoreFile.getMD5().toString().equalsIgnoreCase(file.getImportMd5())  && nextIgnoreFile.getSize().equals(file.getSize())) {
                file.addSimilarFile(getSimilarFile(nextIgnoreFile));
                return true;
            }
        }

        return false;
    }

    private boolean checkOnNameDateOrSize(PreImportFileDTO file) {
        boolean result = false;
        for(IgnoreFile nextIgnoreFile : ignoreFileRepository.findAllByOrderByIdAsc()) {
            int score = 0;

            // Is there a name match?
            if(nextIgnoreFile.getName().equalsIgnoreCase(file.getFilename()) ||
                    nextIgnoreFile.getName().equalsIgnoreCase(file.getImportName())) {
                score++;
            }

            // Is the size a match?
            if(nextIgnoreFile.getSize().equals(file.getSize()) ||
                    nextIgnoreFile.getSize().equals(file.getImportSize())) {
                score++;
            }

            // Does the date match?
            if(nextIgnoreFile.getDate().equals(file.getDate()) ||
                    nextIgnoreFile.getDate().equals(file.getImportDate())) {
                score++;
            }

            // If two criteria match then mark as a potential similar.
            if(score >= 2) {
                result = true;
                file.addSimilarFile(getSimilarFile(nextIgnoreFile));
            }
        }

        return result;
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        // Does the file match MD5 (then don't check anything else.
        if(checkOnMd5(file)) {
            return TrafficLightType.TL_RED;
        }

        if(checkOnNameDateOrSize(file)) {
            return TrafficLightType.TL_AMBER;
        }

        return TrafficLightType.TL_GREEN;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile record) {
        // Don't save this - it will be checked each time.
        return false;
    }
}
