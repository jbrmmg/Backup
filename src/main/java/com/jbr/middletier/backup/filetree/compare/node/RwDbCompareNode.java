package com.jbr.middletier.backup.filetree.compare.node;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.database.DbNode;
import com.jbr.middletier.backup.filetree.realworld.RwFile;
import com.jbr.middletier.backup.filetree.realworld.RwNode;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class RwDbCompareNode extends FileTreeNode {
    public enum ActionType { NONE, INSERT, UPDATE, DELETE, RECREATE_AS_FILE, RECREATE_AS_DIRECTORY }

    private final RwNode realWorldNode;
    private FileSystemObjectId databaseObjectId;
    private ActionType actionType;
    private final boolean isDirectory;

    private boolean updateRequired(FileInfo dbData, File rwFile) {
        // Check date.
        LocalDateTime fileDate = Instant.ofEpochMilli(rwFile.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime();

        long timeDifference = 100;
        if(dbData.getDate() != null) {
            timeDifference = ChronoUnit.SECONDS.between(fileDate, dbData.getDate());
        }

        return Math.abs(timeDifference) > 5000;
    }

    public RwDbCompareNode(FileTreeNode parent, RwNode realWorldNode, DbNode databaseNode) {
        super(parent);

        this.realWorldNode = realWorldNode;
        this.databaseObjectId = databaseNode.getObjectId();
        this.isDirectory = realWorldNode.isDirectory();

        ActionType calculatedActionType = ActionType.NONE;
        if(realWorldNode.isDirectory() != databaseNode.isDirectory()) {
            if(realWorldNode.isDirectory()) {
                calculatedActionType = ActionType.RECREATE_AS_DIRECTORY;
            } else {
                calculatedActionType = ActionType.RECREATE_AS_FILE;
            }
        } else if (!realWorldNode.isDirectory()) {
            // They are equal on name, but check the size, date and MD5 if that is required.
            FileInfo dbFileInfo = (FileInfo)databaseNode.getFSO();
            RwFile rwFile = (RwFile)realWorldNode;

            if(updateRequired(dbFileInfo, rwFile.getFile())) {
                calculatedActionType = ActionType.UPDATE;
            }
        }

        this.actionType = calculatedActionType;
    }

    public RwDbCompareNode(FileTreeNode parent, FileSystemObjectId databaseObjectId) {
        super(parent);

        this.realWorldNode = null;
        this.databaseObjectId = databaseObjectId;
        this.actionType = ActionType.DELETE;
        this.isDirectory = databaseObjectId.getType().equals(FileSystemObjectType.FSO_DIRECTORY);
    }

    public RwDbCompareNode(FileTreeNode parent, RwNode realWorldNode) {
        super(parent);

        this.realWorldNode = realWorldNode;
        this.databaseObjectId = null;
        this.actionType = ActionType.INSERT;
        this.isDirectory = realWorldNode.isDirectory();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // Nothing to do for this type
    }

    public ActionType getActionType() {
        return this.actionType;
    }

    public RwNode getRealWorldNode() {
        return this.realWorldNode;
    }

    public FileSystemObjectId getDatabaseObjectId() {
        return this.databaseObjectId;
    }

    public void setDatabaseObjectId(FileSystemObject databaseObject) {
        this.databaseObjectId = databaseObject.getIdAndType();
    }

    public boolean isDirectory() {
        return this.isDirectory;
    }

    public File getFileForDelete() {
        if( !(realWorldNode instanceof RwFile)) {
            return null;
        }

        RwFile file = (RwFile)realWorldNode;

        this.actionType = ActionType.DELETE;
        return file.getFile();
    }
}
