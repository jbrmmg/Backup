package com.jbr.middletier.backup.data;

public enum ClassificationActionType {
    CA_BACKUP("BACKUP"),
    CA_DELETE("DELETE"),
    CA_FOLDER("FOLDER"),
    CA_IGNORE("IGNORE"),
    CA_WARN("WARN");

    private final String type;

    ClassificationActionType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return this.type;
    }

    public static ClassificationActionType getClassificationActionType(String name) {
        for(ClassificationActionType type : ClassificationActionType.values()) {
            if(type.getTypeName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalStateException(name + " is not a valid Classification Action");
    }
}
