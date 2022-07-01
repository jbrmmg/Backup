package com.jbr.middletier.backup.dto;

@SuppressWarnings("unused")
public class GatherDataDTO extends ProcessResultDTO {
    public enum GatherDataCountType {
        FILES_INSERTED("filesInserted"),
        DIRECTORIES_INSERTED("directoriesInserted"),
        FILES_REMOVED("filesRemoved"),
        DIRECTORIES_REMOVED("directoriesRemoved"),
        DELETES("deletes");

        private final String type;

        GatherDataCountType(String type) {
            this.type = type;
        }

        public String getTypeName() {
            return this.type;
        }
    }

    public GatherDataDTO(int sourceId) {
        super(sourceId);

        // Initialise counts to zero.
        getCount(GatherDataCountType.FILES_INSERTED);
        getCount(GatherDataCountType.DIRECTORIES_INSERTED);
        getCount(GatherDataCountType.FILES_REMOVED);
        getCount(GatherDataCountType.DIRECTORIES_REMOVED);
        getCount(GatherDataCountType.DELETES);
    }

    public void increment(GatherDataCountType countType) { increment(countType.getTypeName()); }

    @SuppressWarnings("UnusedReturnValue")
    public int getCount(GatherDataCountType countType) { return getCount(countType.getTypeName()); }
}
