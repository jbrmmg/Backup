package com.jbr.middletier.backup.filetree.compare;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.database.DbDirectory;
import com.jbr.middletier.backup.filetree.database.DbRoot;

import java.util.List;

public class DbTree extends CompareRoot {
    @Override
    protected FileTreeNode createCompareNode(CompareStatusType status, FileTreeNode parent, FileTreeNode lhs, FileTreeNode rhs) {
        return null;
    }

    @Override
    public List<FileTreeNode> getOrderedNodeList() {
        return null;
    }

    public void compare(DbRoot source, DbDirectory destination) {
        internalCompare(source, destination);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {

    }
}
