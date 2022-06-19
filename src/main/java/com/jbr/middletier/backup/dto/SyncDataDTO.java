package com.jbr.middletier.backup.dto;

@SuppressWarnings("unused")
public class SyncDataDTO extends ProcessResultDTO {
    public enum SyncDataCountType {
        FILES_COPIED("filesCopied"),
        DIRECTORIES_COPIED("directoriesCopied"),
        FILES_DELETED("filesDeleted"),
        DIRECTORIES_DELETED("directoriesDeleted");

        private final String type;

        SyncDataCountType(String type) {
            this.type = type;
        }

        public String getTypeName() {
            return this.type;
        }
    }

    public SyncDataDTO(int underlyingId) {
        super(underlyingId);

        // initialise counts to zero
        getCount(SyncDataCountType.FILES_COPIED);
        getCount(SyncDataCountType.DIRECTORIES_COPIED);
        getCount(SyncDataCountType.FILES_DELETED);
        getCount(SyncDataCountType.DIRECTORIES_DELETED);
    }

    public void increment(SyncDataCountType countType) { increment(countType.getTypeName()); }

    public int getCount(SyncDataCountType countType) { return getCount(countType.getTypeName()); }
}
