package com.jbr.middletier.backup.data;

public enum ActionConfirmType {
    AC_DELETE("DELETE"),
    AC_IMPORT("IMPORT"),
    AC_DELETE_DUPLICATE("DELETE_DUP");

    private final String type;

    ActionConfirmType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return this.type;
    }

    public static ActionConfirmType getActionConfirmType(String name) {
        for(ActionConfirmType type : ActionConfirmType.values()) {
            if(type.getTypeName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalStateException(name + " is not a valid Action Confirm type");
    }
}
