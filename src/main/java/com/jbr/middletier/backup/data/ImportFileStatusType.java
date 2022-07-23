package com.jbr.middletier.backup.data;

public enum ImportFileStatusType {
    IFS_READ("READ"),
    IFS_AWAITING_ACTION("AWAITING_ACTION"),
    IFS_COMPLETE("COMPLETE"),
    IFS_REMOVED("REMOVED");

    private final String type;

    ImportFileStatusType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return this.type;
    }

    public static ImportFileStatusType getFileStatusType(String name) {
        for(ImportFileStatusType type : ImportFileStatusType.values()) {
            if(type.getTypeName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalStateException(name + " is not a valid Import File Status");
    }
}
