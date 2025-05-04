package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

public abstract class ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(ImportStep.class);

    protected final ImportFileRepository importFileRepository;
    protected final ImportSourceManager importSourceManager;

    protected ImportStep(ImportFileRepository importFileRepository,
                         ImportSourceManager importSourceManager) {
        LOG.trace("Creating ImportStep");
        this.importFileRepository = importFileRepository;
        this.importSourceManager = importSourceManager;
    }

    protected abstract boolean transferData(PreImportFileDTO file, ImportFile dbRecord);

    private ImportFile getDbRecord(PreImportFileDTO file) {
        // First, get the record by ID if its present.
        if(file.getId() != null) {
            Optional<ImportFile> importFileOptional = importFileRepository.findById(file.getId());

            if(importFileOptional.isPresent()) {
                return importFileOptional.get();
            }
        }

        // Get the record by name.
        for(FileInfo next : importFileRepository.findByName(file.getFilename())) {
            if(next instanceof ImportFile importFile) {
                return importFile;
            }
        }

        ImportFile newFile = new ImportFile();
        newFile.setName(file.getFilename());

        return newFile;
    }

    protected void saveData(PreImportFileDTO file) {
        // Get the record from the database?
        ImportFile  importFile = getDbRecord(file);

        // Transfer the data, and if required, save it.
        if(transferData(file, importFile)) {
            LOG.info("Saving data for {}", file.getFilename());
            importFileRepository.save(importFile);
        }
    }

    protected File getPreImportFilename(PreImportFileDTO file) {
        return new File(this.importSourceManager.getPreImportDirectory(), file.getFilename());
    }

    protected File getImportFilename(String filename) {
        return new File(this.importSourceManager.getImportDirectory(), filename);
    }

    public abstract FileProcessingStepType getStepType();

    public abstract TrafficLightType performStep(PreImportFileDTO file);
}
