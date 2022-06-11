package com.jbr.middletier.backup.filetree.compare.node;

import com.jbr.middletier.backup.control.ActionController;
import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.database.DbNode;
import com.jbr.middletier.backup.filetree.realworld.RwFile;
import com.jbr.middletier.backup.filetree.realworld.RwNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

public class RwDbCompareNode extends FileTreeNode {
    private static final Logger LOG = LoggerFactory.getLogger(RwDbCompareNode.class);

    public enum ActionType { NONE, INSERT, DELETE, RECREATE_AS_FILE, RECREATE_AS_DIRECTORY }

    private final RwNode realWorldNode;
    private FileSystemObjectId databaseObjectId;
    private ActionType actionType;
    private final boolean isDirectory;

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

    public boolean deleteRwFile() {
        if( !(realWorldNode instanceof RwFile)) {
            return false;
        }

        boolean result = false;
        RwFile file = (RwFile)realWorldNode;

        try {
            Files.deleteIfExists(file.getFile().toPath());
            result = true;
        } catch (IOException e) {
            LOG.warn("Failed to delete file {}", file.getFile());
        }

        this.actionType = ActionType.DELETE;
        return result;
    }
}
