package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.ImportProcessDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.dto.ProcessResultDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileProcessor;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
public class CopyToImport extends ReadPreImportFile {
    private static final Logger LOG = LoggerFactory.getLogger(CopyToImport.class);

    private final ApplicationProperties applicationProperties;

    @Autowired
    protected CopyToImport(ImportFileRepository importFileRepository,
                           AssociatedFileDataManager associatedFileDataManager,
                           FileSystem fileSystem,
                           ApplicationProperties applicationProperties) {
        super(fileSystem, importFileRepository, associatedFileDataManager);
        this.applicationProperties = applicationProperties;
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_COPY_FILE_TO_IMPORT;
    }

    private String getCopiedFilename(PreImportFileDTO file) {
        // MOV files are converted to mp4.
        if(file.getFilename().toLowerCase().endsWith(".mov")) {
            return file.getFilename().substring(0, file.getFilename().length() - 4) + ".mp4";
        }

        return file.getFilename();
    }

    private void setDateTime(File destination, PreImportFileDTO file, long defaultTime) {
        if(file != null && file.getImportDate() != null) {
            ZonedDateTime zonedDateTime = file.getImportDate().atZone(ZoneId.systemDefault());
            defaultTime = zonedDateTime.toInstant().toEpochMilli();
        }
        fileSystem.setFileDateTime(destination, defaultTime);
    }

    private TrafficLightType copyConvertQuicktime(File source, File destination, PreImportFileDTO file) {
        try {
            long fileTime = source.lastModified();

            if(destination.exists()) {
                Files.deleteIfExists(destination.toPath());
            }

            String copyCommand = applicationProperties.getFfmpegCommand();
            copyCommand = copyCommand.replace("%%INPUT%%", source.toString().replace(" ", "\\ "));
            copyCommand = copyCommand.replace("%%OUTPUT%%", destination.toString().replace(" ", "\\ "));

            LOG.info("Command: {}", copyCommand);

            String[] cmd = new String[]{"bash", "-c", copyCommand};
            final Process backupProcess = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            backupProcess.waitFor(20L, TimeUnit.MINUTES);
            backupProcess.destroyForcibly();

            setDateTime(destination, file, fileTime);
        } catch (Exception e) {
            LOG.error("Failed to copy MOV file", e);
            Thread.currentThread().interrupt();
            return TrafficLightType.TL_RED;
        }

        return TrafficLightType.TL_GREEN;
    }

    private TrafficLightType standardCopy(File source, File destination, PreImportFileDTO file) {
        long fileTime = source.lastModified();

        ProcessResultDTO data = new ImportProcessDTO();
        fileSystem.copyFile(source, destination, data);

        if(data.hasProblems()) {
            return TrafficLightType.TL_RED;
        }

        // If the file is provided then get the date/time.
        setDateTime(destination, file, fileTime);
        return TrafficLightType.TL_GREEN;
    }

    private TrafficLightType copyFile(File source, File destination, PreImportFileDTO file) {
        // Determine the copy type.
        if(file.getFilename().toLowerCase().endsWith(".mov")) {
            return copyConvertQuicktime(source, destination, file);
        }

        if(file.isVideo() || file.isImage()) {
            return standardCopy(source, destination, file);
        }

        return standardCopy(source, destination, null);
    }

    private void gatherDataOfImport(File importFile, PreImportFileDTO file) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Gather the details of the file.
        LocalDateTime importDate = FileProcessor.getFileLastModified(importFile);
        LOG.info("Gathering import data of {} {}", file.getFilename(), formatter.format(importDate));
        file.setImportSize(importFile.length());
        file.setImportDate(importDate);
        getMD5(file, importFile, true);
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        LOG.info("Checking if file has been copied to import");

        // Has this file already been copied?
        String copyFile = getCopiedFilename(file);
        File destinationFile = getImportFilename(copyFile);
        LOG.info("Check file {} is copied to {}", file.getFilename(), copyFile);
        if(destinationFile.exists() && file.getImportMd5() != null && file.getImportName() != null && file.getImportDate() != null && file.getImportSize() != null) {
            file.setInImport(true);
            return TrafficLightType.TL_GREEN;
        }

        file.setImportName(copyFile);
        File sourceFile = getPreImportFilename(file);
        LOG.info("Copy file {} to {}", sourceFile, destinationFile);

        // This step requires that the metadata has been read.
        TrafficLightType metaDataStatus = file.getStepStatus(FileProcessingStepType.FPS_GATHER_META_DATA);
        if(!metaDataStatus.equals(TrafficLightType.TL_GREEN)) {
            return TrafficLightType.TL_RED;
        }

        // Copy the file
        TrafficLightType copyStatus = copyFile(sourceFile, destinationFile, file);
        if(!copyStatus.equals(TrafficLightType.TL_GREEN)) {
            return copyStatus;
        }

        gatherDataOfImport(destinationFile, file);
        file.setInImport(true);
        saveData(file);

        if(file.getImportMd5() != null) {
            return TrafficLightType.TL_GREEN;
        }

        return TrafficLightType.TL_AMBER;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile record) {
        if(record.getImportName() == null) {
            LOG.info("Save details of the import file {}", file.getFilename());
            record.setImportName(file.getImportName());
            record.setImportMd5(file.getImportMd5());
            record.setImportSize(file.getImportSize());
            record.setImportDate(file.getImportDate());
            return true;
        }

        return false;
    }
}
