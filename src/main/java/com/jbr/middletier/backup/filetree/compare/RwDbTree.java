package com.jbr.middletier.backup.filetree.compare;

import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.compare.node.RwDbCompareNode;
import com.jbr.middletier.backup.filetree.database.DbNode;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import com.jbr.middletier.backup.filetree.realworld.RwNode;
import com.jbr.middletier.backup.filetree.realworld.RwRoot;

import java.util.ArrayList;
import java.util.List;

public class RwDbTree extends CompareRoot {
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

        RwNode rwLhsNode = (RwNode)lhs;
        if(CompareStatusType.REMOVED.equals(status)) {
            return new RwDbCompareNode(parent,rwLhsNode);
        }

        DbNode dbRhsNode = (DbNode)rhs;
        if(CompareStatusType.ADDED.equals(status)) {
            return new RwDbCompareNode(parent,dbRhsNode.getObjectId());
        }

        return new RwDbCompareNode(parent,rwLhsNode,dbRhsNode);
    }

    private void findDeleteFiles(FileTreeNode node, List<FileTreeNode> result) {
        for(FileTreeNode next : node.getChildren()) {
            findDeleteFiles(next,result);
        }

        // Ignore nodes that are not the right type.
        if(!(node instanceof RwDbCompareNode)) {
            return;
        }

        RwDbCompareNode compareNode = (RwDbCompareNode)node;
        if(compareNode.isDirectory() && !compareNode.getActionType().equals(RwDbCompareNode.ActionType.RECREATE_AS_DIRECTORY)) {
            return;
        }

        // If this is a file, add a delete then add to the list now.
        if(compareNode.getActionType().equals(RwDbCompareNode.ActionType.DELETE) ||
            compareNode.getActionType().equals(RwDbCompareNode.ActionType.RECREATE_AS_DIRECTORY) ) {
            result.add(compareNode);
        }
    }

    @Override
    public List<FileTreeNode> getOrderedNodeList() {
        // Nodes are placed in this order in the list:
        //
        // DELETE file      - delete file details from DB (order not important).
        // DELETE directory - delete directory details from DB (highest level first).
        // INSERT directory - insert directory details (lowest level first).
        // INSERT file      - insert files (order not important).

        List<FileTreeNode> result = new ArrayList<>();

        // Get the nodes that represent a delete file.
        findDeleteFiles(this,result);

        // Get the nodes that represent a delete directory.
        findDeleteDirectories();

        // Get the nodes that represent an insert directory.

        // Get the nodes that represent an insert file.

        return result;
    }

    public void compare(RwRoot realWorld, DbRoot database) {
        internalCompare(realWorld, database);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // Not required on this.
    }
}
