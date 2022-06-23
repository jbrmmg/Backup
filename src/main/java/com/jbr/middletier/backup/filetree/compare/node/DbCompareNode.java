package com.jbr.middletier.backup.filetree.compare.node;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.database.DbFile;
import com.jbr.middletier.backup.filetree.database.DbNode;
import com.jbr.middletier.backup.filetree.database.DbNodeCompareResultType;

public class DbCompareNode  extends FileTreeNode {
    public enum ActionType { NONE, COPY, REMOVE, RECREATE_AS_FILE, RECREATE_AS_DIRECTORY }
    public enum SubActionType { NONE, WARN, IGNORE, REMOVE_SOURCE, DATE_UPDATE }

    private final ActionType actionType;
    private final SubActionType subActionType;
    private final boolean isDirectory;
    private final DbNode source;
    private final DbNode destination;

    private SubActionType getSubActionCopy(DbNode source, DbNode destination) {
        // For copy there must be a source.
        if(source instanceof DbFile) {
            DbFile file = (DbFile) source;
            if(file.getClassification() != null) {
                switch (file.getClassification().getAction()) {
                    case CA_WARN:
                        return SubActionType.WARN;
                    case CA_IGNORE:
                        return SubActionType.IGNORE;
                    case CA_DELETE:
                        return SubActionType.REMOVE_SOURCE;
                    default:
                        // Nothing further, continue.
                }
            }
        }

        // If no destination, then not sub action
        if(destination == null) {
            return SubActionType.NONE;
        }

        if(source instanceof DbFile) {
            DbFile file = (DbFile) source;
            if (file.compare(destination) == DbNodeCompareResultType.DBC_EQUAL_EXCEPT_DATE) {
                return SubActionType.DATE_UPDATE;
            }
        }

        return SubActionType.NONE;
    }

    private SubActionType getSubActionRecreateAsFile(DbNode source, DbNode destination) {
        return getSubActionCopy(source, destination);
    }

    private SubActionType getSubAction(ActionType action, DbNode source, DbNode destination) {
        switch(action) {
            case COPY:
                return getSubActionCopy(source,destination);
            case RECREATE_AS_FILE:
                return getSubActionRecreateAsFile(source,destination);
            default:
                // Nothing further, continue.
        }

        return SubActionType.NONE;
    }

    private ActionType getAction(DbNode source, DbNode destination) {
        // No source - remove
        if(source == null) {
            return ActionType.REMOVE;
        }

        // No destination - copy
        if(destination == null) {
            return ActionType.COPY;
        }

        // There is a source and destination - compare
        if(source.isDirectory() == destination.isDirectory()) {
            switch(source.compare(destination)) {
                case DBC_NOT_EQUAL:
                case DBC_EQUAL_EXCEPT_DATE:
                    return ActionType.COPY;
                default:
                    // Nothing further, continue.
            }
        } else if(source.isDirectory()) {
            return ActionType.RECREATE_AS_DIRECTORY;
        } else {
            return ActionType.RECREATE_AS_FILE;
        }

        return ActionType.NONE;
    }

    public DbCompareNode(FileTreeNode parent, DbNode source, DbNode destination) {
        super(parent);

        this.isDirectory = source != null ? source.isDirectory() : destination.isDirectory();
        this.actionType = getAction(source, destination);
        this.subActionType = getSubAction(this.actionType, source, destination);
        this.source = source;
        this.destination = destination;
    }

    public DbCompareNode(FileTreeNode parent, boolean source, DbNode sourceOrDestination) {
        super(parent);
        this.isDirectory = sourceOrDestination.isDirectory();
        if(source) {
            this.actionType = getAction(sourceOrDestination, null);
            this.subActionType = getSubAction(this.actionType, sourceOrDestination, null);
            this.source = sourceOrDestination;
            this.destination = null;
        } else {
            this.actionType = getAction(null, sourceOrDestination);
            this.subActionType = getSubAction(this.actionType,null, sourceOrDestination);
            this.source = null;
            this.destination = sourceOrDestination;
        }
    }

    public ActionType getActionType() {
        return this.actionType;
    }

    public SubActionType getSubActionType() {
        return this.subActionType;
    }

    public boolean isDirectory() {
        return this.isDirectory;
    }

    public DbNode getSource() {
        return this.source;
    }

    public DbNode getDestination() {
        return this.destination;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // Nothing required.
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(this.actionType);
        result.append(" ");
        result.append(this.subActionType);

        if(this.source != null) {
            result.append(" ");
            result.append(this.source.getFSO().getIdAndType());
        }

        if(this.destination != null) {
            result.append(" ");
            result.append(this.destination.getFSO().getIdAndType());
        }

        return result.toString();
    }
}
