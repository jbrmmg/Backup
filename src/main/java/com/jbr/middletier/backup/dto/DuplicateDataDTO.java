package com.jbr.middletier.backup.dto;

@SuppressWarnings("unused")
public class DuplicateDataDTO extends ProcessResultDTO {
    public enum DuplicateCountType {
        CHECKED("checked"),
        DELETED("deleted");

        private final String type;

        DuplicateCountType(String type) {
            this.type = type;
        }

        public String getTypeName() {
            return this.type;
        }
    }

    public DuplicateDataDTO(int sourceId) {
        super(sourceId);

        // initialise counts to zero
        getCount(DuplicateCountType.CHECKED);
        getCount(DuplicateCountType.DELETED);
    }

    public void increment(DuplicateCountType countType) { increment(countType.getTypeName()); }

    public int getCount(DuplicateCountType countType) { return getCount(countType.getTypeName()); }
}
