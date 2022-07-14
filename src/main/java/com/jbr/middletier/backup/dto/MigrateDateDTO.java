package com.jbr.middletier.backup.dto;

public class MigrateDateDTO extends ProcessResultDTO {
    public enum MigrateDataCountType {
        BLANKS_REMOVED("blanksRemoved"),
        DOT_FILES_REMOVED("dotFilesRemoved"),
        DIRECTORIES_UPDATED("directoriesUpdated"),
        NEW_DIRECTORIES("newDirectories");

        private final String type;

        MigrateDataCountType(String type) {
            this.type = type;
        }

        public String getTypeName() {
            return this.type;
        }
    }

    public MigrateDateDTO() {
        super(-1);

        // Initialise counts to zero.
        getCount(MigrateDateDTO.MigrateDataCountType.BLANKS_REMOVED);
        getCount(MigrateDateDTO.MigrateDataCountType.DOT_FILES_REMOVED);
        getCount(MigrateDateDTO.MigrateDataCountType.DIRECTORIES_UPDATED);
        getCount(MigrateDateDTO.MigrateDataCountType.NEW_DIRECTORIES);
    }

    public void increment(MigrateDateDTO.MigrateDataCountType countType) { increment(countType.getTypeName()); }

    @SuppressWarnings("UnusedReturnValue")
    public int getCount(MigrateDateDTO.MigrateDataCountType countType) { return getCount(countType.getTypeName()); }
}
