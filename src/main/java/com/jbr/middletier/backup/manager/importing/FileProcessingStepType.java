package com.jbr.middletier.backup.manager.importing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum FileProcessingStepType {
    FPS_READ_PREIMPORT_FILE (0),
    FPS_GATHER_META_DATA (1),
    FPS_COPY_FILE_TO_IMPORT (2),
    FPS_CHECK_FILE_IGNORED (3),
    FPS_CHECK_ACTIVE_PHOTO_FILE (4),
    FPS_CHECK_DUPLICATE_FILE (5),
    FPS_CHECK_FILE_CONFIRMED_IMPORTED (6),
    FPS_FINAL_UPDATE (7);

    private final Integer value;
    FileProcessingStepType(int value) {
        this.value = value;
    }

    public static List<FileProcessingStepType> getStepsInOrder() {
        List<FileProcessingStepType> result = new ArrayList<>();

        Collections.addAll(result, FileProcessingStepType.values());
        result.sort(Comparator.comparing(l -> l.value));

        return result;
    }
}
