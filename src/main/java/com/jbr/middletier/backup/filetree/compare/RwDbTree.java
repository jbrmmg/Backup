package com.jbr.middletier.backup.filetree.compare;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.compare.node.RwDbCompareNode;
import com.jbr.middletier.backup.filetree.database.DbNode;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import com.jbr.middletier.backup.filetree.realworld.RwNode;
import com.jbr.middletier.backup.filetree.realworld.RwRoot;
import java.util.List;
import java.util.Optional;

public class RwDbTree extends CompareRoot {
    private final RwRoot realWorld;
    private final DbRoot database;

    public RwDbTree(RwRoot realWorld, DbRoot database) {
        this.realWorld = realWorld;
        this.database = database;
    }

    @Override
    protected FileTreeNode createCompareNode(CompareStatusType status, FileTreeNode parent, FileTreeNode lhs, FileTreeNode rhs) {
        // Possible status is EQUAL, REMOVED (needs to be added to DB) or ADDED (needs to be removed from the DB).
        // If they are marked equal but are of different types then potentially need to delete and re-add.

        // REMOVED
        // Require the real world information.

        // ADDED
        // Require the database information - in fact just the id & type.

        // EQUAL (and both are same type) - potential delete - need the real world and the id and type.
        //

        // EQUAL - RealWorld is a directory, Database is a file.  Need the real world and the id and type.
        //         Need to delete the file and create a directory.

        // EQUAL - RealWorld is a file, Database is a directory.  Need the real world and the id and type.
        //         Need to delete the directory and create a file.

        if(CompareStatusType.REMOVED.equals(status)) {
            return new RwDbCompareNode(parent,(RwNode)lhs);
        }

        if(CompareStatusType.ADDED.equals(status)) {
            DbNode dbRhsNode = (DbNode)rhs;
            return new RwDbCompareNode(parent,dbRhsNode.getObjectId());
        }

        return new RwDbCompareNode(parent,(RwNode)lhs,(DbNode)rhs);
    }

    @Override
    protected void findDeleteFiles(FileTreeNode node, List<FileTreeNode> result) {
        for(FileTreeNode next : node.getChildren()) {
            findDeleteFiles(next,result);
        }

        // Ignore nodes that are not the right type.
        if(!(node instanceof RwDbCompareNode compareNode)) {
            return;
        }

        // If this is a delete and not a directory, or a recreate as directory.
        if((compareNode.getActionType().equals(RwDbCompareNode.ActionType.DELETE) && !compareNode.isDirectory()) ||
                compareNode.getActionType().equals(RwDbCompareNode.ActionType.RECREATE_AS_DIRECTORY)) {
            result.add(compareNode);
        }
    }

    @Override
    protected void findDeleteDirectories(FileTreeNode node, List<FileTreeNode> result) {
        for(FileTreeNode next : node.getChildren()) {
            findDeleteDirectories(next,result);
        }

        // Ignore nodes that are not the right type.
        if(!(node instanceof RwDbCompareNode compareNode)) {
            return;
        }

        // If this is a delete and a directory, or a recreate as file.
        if((compareNode.getActionType().equals(RwDbCompareNode.ActionType.DELETE) && compareNode.isDirectory()) ||
                compareNode.getActionType().equals(RwDbCompareNode.ActionType.RECREATE_AS_FILE)) {
            result.add(compareNode);
        }
    }

    @Override
    protected void findInsertDirectories (FileTreeNode node, List<FileTreeNode> result) {
        // If this is an insert and not a directory, or recreate as file.
        if((node instanceof RwDbCompareNode compareNode) &&
            ((compareNode.getActionType().equals(RwDbCompareNode.ActionType.INSERT) && compareNode.isDirectory()) ||
                    compareNode.getActionType().equals(RwDbCompareNode.ActionType.RECREATE_AS_DIRECTORY))) {
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
        if(!(node instanceof RwDbCompareNode compareNode)) {
            return;
        }

        // If this is a file, add a delete then add to the list now.
        if((compareNode.getActionType().equals(RwDbCompareNode.ActionType.INSERT) && !compareNode.isDirectory()) ||
                (compareNode.getActionType().equals(RwDbCompareNode.ActionType.UPDATE) && !compareNode.isDirectory()) ||
                compareNode.getActionType().equals(RwDbCompareNode.ActionType.RECREATE_AS_FILE)) {
            result.add(compareNode);
        }
    }

    public void compare() {
        internalCompare(this.realWorld, this.database);
    }

    @Override
    public Optional<String> getName() {
        return Optional.empty();
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // Not required on this.
    }

    public DbRoot getDbSource() {
        return this.database;
    }
}
