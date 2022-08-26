package com.jbr.middletier.backup.data;

public enum DbLogType {
    DLT_DEBUG("DBG", "Debug"),
    DLT_INFO("INF", "Info"),
    DLT_WARNING("WRN", "Warning"),
    DLT_ERROR("ERR", "Error");

    private final String type;
    private final String displayName;

    DbLogType(String type, String displayName) {
        this.type = type;
        this.displayName = displayName;
    }

    public String getTypeName() {
        return this.type;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static DbLogType getDbLogType(String name) {
        for(DbLogType type : DbLogType.values()) {
            if(type.getTypeName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalStateException(name + " is not a valid Log type");
    }
}
