package com.jbr.middletier.backup.data;

public enum SourceStatusType {
    SST_OK("OK"),
    SST_GATHERING("GATHERING"),
    SST_ERROR("ERROR");

    private final String type;

    SourceStatusType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return this.type;
    }

    public static SourceStatusType getSourceStatusType(String name) {
        if(name == null)
            return null;

        for(SourceStatusType type : SourceStatusType.values()) {
            if(type.getTypeName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalStateException(name + " is not a valid Source Status type");
    }
}
