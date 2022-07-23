package com.jbr.middletier.backup.dto;

public class ImportProcessDTO extends ProcessResultDTO {
    public enum ImportProcessCountType {
        FILES_PROCESSED("filesProcessed"),
        IMAGE_FILES("imageFiles"),
        MOV_FILES("movFiles"),
        ALREADY_PRESENT("alreadyPresent");

        private final String type;

        ImportProcessCountType(String type) {
            this.type = type;
        }

        public String getTypeName() {
            return this.type;
        }
    }

    public ImportProcessDTO() {
        super(-1);

        // Initialise counts to zero.
        getCount(ImportProcessDTO.ImportProcessCountType.FILES_PROCESSED);
        getCount(ImportProcessDTO.ImportProcessCountType.IMAGE_FILES);
        getCount(ImportProcessDTO.ImportProcessCountType.MOV_FILES);
        getCount(ImportProcessDTO.ImportProcessCountType.ALREADY_PRESENT);
    }

    public void increment(ImportProcessDTO.ImportProcessCountType countType) { increment(countType.getTypeName()); }

    @SuppressWarnings("UnusedReturnValue")
    public int getCount(ImportProcessDTO.ImportProcessCountType countType) { return getCount(countType.getTypeName()); }
}
