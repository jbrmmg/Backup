package com.jbr.middletier.backup.filetree.compare;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;

import java.util.ArrayList;
import java.util.List;

public abstract class CompareRoot extends RootFileTreeNode {
    protected abstract FileTreeNode createCompareNode(CompareStatusType status, FileTreeNode parent, FileTreeNode lhs, FileTreeNode rhs);

    protected void performCompare(FileTreeNode result, FileTreeNode lhs, FileTreeNode rhs) {
        List<String> added = new ArrayList<>();

        for(FileTreeNode nextLHS : getChildren(lhs)) {
            for(FileTreeNode nextRHS : getChildren(rhs)) {
                if(nextLHS.getName().equals(nextRHS.getName())) {
                    added.add(nextLHS.getName());
                    FileTreeNode resultNode = createCompareNode(CompareStatusType.EQUAL, result, nextLHS, nextRHS);
                    getChildren(result).add(resultNode);
                    performCompare(resultNode,nextLHS,nextRHS);
                }
            }

            if(!added.contains(nextLHS.getName())) {
                added.add(nextLHS.getName());
                FileTreeNode resultNode = createCompareNode(CompareStatusType.REMOVED, result, nextLHS, nullNode);
                getChildren(result).add(resultNode);
            }
        }

        for(FileTreeNode nextRHS : getChildren(rhs)) {
            if(!added.contains(nextRHS.getName())) {
                added.add(nextRHS.getName());
                FileTreeNode resultNode = createCompareNode(CompareStatusType.ADDED, result, nullNode, nextRHS);
                getChildren(result).add(resultNode);
            }
        }
    }

    protected void internalCompare(FileTreeNode lhs, FileTreeNode rhs) {
        performCompare(this, lhs, rhs);
    }

    public abstract List<FileTreeNode> getOrderedNodeList();
}
