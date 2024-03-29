package com.jbr.middletier.backup.filetree.compare;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.compare.node.DbCompareNode;
import com.jbr.middletier.backup.filetree.database.DbNode;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import java.util.List;
import java.util.Optional;

public class DbTree extends CompareRoot {
    private final DbRoot source;
    private final DbRoot destination;

    public DbTree(DbRoot source, DbRoot destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    protected FileTreeNode createCompareNode(CompareStatusType status, FileTreeNode parent, FileTreeNode source, FileTreeNode destination) {
        // Possible status is EQUAL, REMOVED (needs to be added to destination) or ADDED (needs to be removed from the destination).
        // If they are marked equal but are of different types then potentially need to delete and re-add.

        if(status.equals(CompareStatusType.REMOVED)) {
            if(source instanceof DbNode dbNode) {
                return new DbCompareNode(parent, true, dbNode);
            }

            throw new IllegalStateException("Status is removed, but no source provided");
        }

        if(status.equals(CompareStatusType.ADDED)) {
            if(destination instanceof DbNode dbNode) {
                return new DbCompareNode(parent, false, dbNode);
            }

            throw new IllegalStateException("Status is added, but no destination provided");
        }

        return new DbCompareNode(parent, (DbNode)source, (DbNode)destination);
    }

    @Override
    protected void findDeleteFiles(FileTreeNode node, List<FileTreeNode> result) {
        for(FileTreeNode next : node.getChildren()) {
            findDeleteFiles(next,result);
        }

        // Ignore nodes that are not the right type.
        if(!(node instanceof DbCompareNode compareNode)) {
            return;
        }

        // If this is delete and not a directory, or a re-create as directory.
        if((compareNode.getActionType().equals(DbCompareNode.ActionType.REMOVE) && !compareNode.isDirectory()) ||
                compareNode.getActionType().equals(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY)) {
            result.add(compareNode);
        }
    }

    @Override
    protected void findDeleteDirectories(FileTreeNode node, List<FileTreeNode> result) {
        for(FileTreeNode next : node.getChildren()) {
            findDeleteDirectories(next,result);
        }

        // Ignore nodes that are not the right type.
        if(!(node instanceof DbCompareNode compareNode)) {
            return;
        }

        // If this is a delete and a directory, or a recreate as file.
        if((compareNode.getActionType().equals(DbCompareNode.ActionType.REMOVE) && compareNode.isDirectory()) ||
                compareNode.getActionType().equals(DbCompareNode.ActionType.RECREATE_AS_FILE)) {
            result.add(compareNode);
        }
    }

    @Override
    protected void findInsertDirectories(FileTreeNode node, List<FileTreeNode> result) {
        // If this is an insert and not a directory, or recreate as a file.
        if((node instanceof DbCompareNode compareNode) &&
            ((compareNode.getActionType().equals(DbCompareNode.ActionType.COPY) && compareNode.isDirectory()) ||
                    compareNode.getActionType().equals(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY))) {
            result.add(compareNode);
        }

        for(FileTreeNode next : node.getChildren()) {
            findInsertDirectories(next,result);
        }
    }

    @Override
    protected void findInsertFiles(FileTreeNode node, List<FileTreeNode> result) {
        for(FileTreeNode next : node.getChildren()) {
            findInsertFiles(next,result);
        }

        // Ignore nodes that are not the right type.
        if(!(node instanceof DbCompareNode compareNode)) {
            return;
        }

        // If this is a file, add a delete then add to the list now.
        if((compareNode.getActionType().equals(DbCompareNode.ActionType.COPY) && !compareNode.isDirectory()) ||
                compareNode.getActionType().equals(DbCompareNode.ActionType.RECREATE_AS_FILE)) {
            result.add(compareNode);
        }
    }

    public void compare() {
        internalCompare(source, destination);
    }

    @Override
    public Optional<String> getName() {
        return Optional.empty();
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // Not required for this class.
    }
}
