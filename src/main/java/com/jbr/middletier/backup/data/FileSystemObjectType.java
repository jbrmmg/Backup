package com.jbr.middletier.backup.data;

public enum FileSystemObjectType {
    FSO_IMAGE_FILE("IMGE"),
    FSO_VIDEO_FILE("VIDO"),
    FSO_IGNORE_FILE("IGNO"),
    FSO_IMPORT_FILE("IMPO"),
    FSO_FILE("FILE"),
    FSO_DIRECTORY("DIRY"),
    FSO_SOURCE("SRCE"),
    FSO_IMPORT_SOURCE("IMPS"),
    FSO_PRE_IMPORT_SOURCE("PIMP");

    private final String type;

    FileSystemObjectType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return this.type;
    }

    public static FileSystemObjectType getFileSystemObjectType(String name) {
        for(FileSystemObjectType type : FileSystemObjectType.values()) {
            if(type.getTypeName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalStateException(name + " is not a valid File System Object type");
    }
}
