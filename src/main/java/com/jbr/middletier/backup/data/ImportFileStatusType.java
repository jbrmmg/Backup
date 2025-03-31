package com.jbr.middletier.backup.data;

public enum ImportFileStatusType {
    IFS_READ("READ"),
    IFS_AWAITING_ACTION("AWAITING_ACTION"),
    IFS_COMPLETE("COMPLETE"),
    IFS_REMOVED("REMOVED"),
    IFS_REMOVE_IMPORTED("REMOVE_IMPORTED"),
    IFS_REMOVE_IGNORED("REMOVE_IGNORED"),
    IFS_REMOVE_ACTIVE_PHOTO("REMOVE_ACTIVE_PHOTO"),
    IFS_MANUAL_DELETE("MANUAL_DELETE"),
    IFS_IMPORT_FILE("IMPORT_FILE");

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
