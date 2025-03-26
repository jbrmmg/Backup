package com.jbr.middletier.backup.manager.importing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum FileProcessingStepType {
    FPS_READ_PREIMPORT_FILE (0, "readPreImportFile"),
    FPS_GATHER_META_DATA (1, "gatherMetaData"),
    FPS_COPY_FILE_TO_IMPORT (2, "copyFileToImport"),
    FPS_CHECK_FILE_IGNORED (3, "checkFileIgnored"),
    FPS_CHECK_ACTIVE_PHOTO_FILE (4, "checkActivePhotoFile"),
    FPS_CHECK_DUPLICATE_FILE (5, "checkDuplicateFile"),
    FPS_CHECK_FILE_CONFIRMED_IMPORTED (6, "checkFileConfirmedImported"),
    FPS_PROCESS_IMPORT (7, "processImport"),
    FPS_FINAL_UPDATE (8, "completed");

    private final Integer order;
    private final String jsonName;

    FileProcessingStepType(int order, String jsonName) {
        this.order = order;
        this.jsonName = jsonName;
    }

    public static List<FileProcessingStepType> getStepsInOrder() {
        List<FileProcessingStepType> result = new ArrayList<>();

        Collections.addAll(result, FileProcessingStepType.values());
        result.sort(Comparator.comparing(l -> l.order));

        return result;
    }

    public static String getJsonName(FileProcessingStepType step) {
        for (FileProcessingStepType nextStep : FileProcessingStepType.values()) {
            if(nextStep.equals(step)) {
                return nextStep.jsonName;
            }
        }

        throw new IllegalArgumentException(step.toString() + " is not a valid FileProcessingStepType");
    }
}
