package com.jbr.middletier.backup.dto;

public class ImportDataDTO extends ProcessResultDTO {
    public enum ImportDataCountType {
        NON_BACKUP_CLASSIFICATIONS("nonBackupClassification"),
        IGNORED_IMPORTS("ignoredImports"),
        ALREADY_IMPORTED("alreadyImported"),
        IGNORED("ignored"),
        IMPORTED("imported");

        private final String type;

        ImportDataCountType(String type) {
            this.type = type;
        }

        public String getTypeName() {
            return this.type;
        }
    }

    public ImportDataDTO(int sourceId) {
        super(sourceId);

        // Initialise counts to zero.
        getCount(ImportDataDTO.ImportDataCountType.NON_BACKUP_CLASSIFICATIONS);
        getCount(ImportDataDTO.ImportDataCountType.IGNORED_IMPORTS);
        getCount(ImportDataDTO.ImportDataCountType.ALREADY_IMPORTED);
        getCount(ImportDataDTO.ImportDataCountType.IGNORED);
        getCount(ImportDataDTO.ImportDataCountType.IMPORTED);
    }

    public void increment(ImportDataDTO.ImportDataCountType countType) { increment(countType.getTypeName()); }

    @SuppressWarnings("UnusedReturnValue")
    public int getCount(ImportDataDTO.ImportDataCountType countType) { return getCount(countType.getTypeName()); }
}
