package com.jbr.middletier.backup.manager.importing.step.process;

import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ProcessBase {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessBase.class);

    private final ImportFileStatusType type;
    protected final ImportSourceManager importSourceManager;
    protected final AssociatedFileDataManager associatedFileDataManager;

    protected ProcessBase(ImportFileStatusType type,
                          AssociatedFileDataManager associatedFileDataManager,
                          ImportSourceManager importSourceManager) {
        this.type = type;
        this.importSourceManager = importSourceManager;
        this.associatedFileDataManager = associatedFileDataManager;
    }

    private static List<TrafficLightType> mustBeGreenList = null;
    protected static List<TrafficLightType> getMustBeGreen() {
        if(mustBeGreenList == null){
            mustBeGreenList = new ArrayList<>();
            mustBeGreenList.add(TrafficLightType.TL_GREEN);
        }

        return mustBeGreenList;
    }

    private static List<TrafficLightType> mustBeRedList = null;
    protected static List<TrafficLightType> getMustBeRed() {
        if(mustBeRedList == null){
            mustBeRedList = new ArrayList<>();
            mustBeRedList.add(TrafficLightType.TL_RED);
        }

        return mustBeRedList;
    }

    private static List<TrafficLightType> mustNotBeRedList = null;
    protected static List<TrafficLightType> getMustNotBeRed() {
        if(mustNotBeRedList == null){
            mustNotBeRedList = new ArrayList<>();
            for(TrafficLightType trafficLightType : TrafficLightType.values()){
                if(!trafficLightType.equals(TrafficLightType.TL_RED)){
                    mustNotBeRedList.add(trafficLightType);
                }
            }
        }

        return mustNotBeRedList;
    }

    private static List<TrafficLightType> mustNotBeGreenList = null;
    protected static List<TrafficLightType> getMustNotBeGreen() {
        if(mustNotBeGreenList == null){
            mustNotBeGreenList = new ArrayList<>();
            for(TrafficLightType trafficLightType : TrafficLightType.values()){
                if(!trafficLightType.equals(TrafficLightType.TL_GREEN)){
                    mustNotBeGreenList.add(trafficLightType);
                }
            }
        }

        return mustNotBeGreenList;
    }

    protected File getPreImportFilename(PreImportFileDTO file) {
        return new File(this.importSourceManager.getPreImportDirectory(), file.getFilename());
    }

    protected File getImportFilename(String filename) {
        return new File(this.importSourceManager.getImportDirectory(), filename);
    }

    protected File getPostImportFilename(PreImportFileDTO file) {
        return new File(this.importSourceManager.getPostImportDirectory(), file.getImportName());
    }

    protected void deleteFile(PreImportFileDTO file) throws IOException  {
        // Remove from all import directories PostImport, Import then PreImport.
        File postImportFile = getPostImportFilename(file);
        if(postImportFile.exists()) {
            Files.delete(postImportFile.toPath());
        }

        File importFile = getImportFilename(file.getImportName());
        if(importFile.exists()) {
            Files.delete(importFile.toPath());
        }

        File preImportFile = getPreImportFilename(file);
        if(preImportFile.exists()) {
            Files.delete(preImportFile.toPath());
        }

        // Mark this file as deleted.
        file.setStatus(ImportFileStatusType.IFS_REMOVED);
    }

    protected void validateStepStatus(PreImportFileDTO file, Map<FileProcessingStepType, List<TrafficLightType>> requiredStepStatus) throws ImportProcessException {
        // Step status must be in the status values listed - if they are specified.
        for(Map.Entry<FileProcessingStepType, List<TrafficLightType>> entry : requiredStepStatus.entrySet()) {
            TrafficLightType status = file.getStepStatus(entry.getKey());

            // This status must be in the list.
            boolean validStatus = false;
            for(TrafficLightType trafficLightType : entry.getValue()) {
                if (status.equals(trafficLightType)) {
                    validStatus = true;
                    break;
                }
            }

            if(!validStatus){
                LOG.warn("{} invalid step {} status {}",file.getFilename(), entry.getKey(), status);
                throw new ImportProcessException(file.getFilename() + " invalid step " + entry.getKey() + " status " + status);
            }
        }
    }

    public ImportFileStatusType getType() {
        return type;
    }

    public abstract TrafficLightType process(PreImportFileDTO file) throws ImportProcessException;
}
