package com.jbr.middletier.backup.filetree.compare;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;
import com.jbr.middletier.backup.filetree.compare.node.SectionNode;

import java.util.ArrayList;
import java.util.List;

public abstract class CompareRoot extends RootFileTreeNode {
    protected abstract FileTreeNode createCompareNode(CompareStatusType status, FileTreeNode parent, FileTreeNode lhs, FileTreeNode rhs);

    protected void performCompare(FileTreeNode result, FileTreeNode lhs, FileTreeNode rhs) {
        List<String> added = new ArrayList<>();

        for(FileTreeNode nextLHS : lhs.getChildren()) {
            String lhsName = nextLHS.getName().orElse("");
            if(lhsName.length() == 0) {
                continue;
            }

            for(FileTreeNode nextRHS : rhs.getChildren()) {
                String rhsName = nextRHS.getName().orElse("");
                if(rhsName.length() == 0) {
                    continue;
                }

                if(lhsName.equals(rhsName)) {
                    added.add(lhsName);
                    FileTreeNode resultNode = createCompareNode(CompareStatusType.EQUAL, result, nextLHS, nextRHS);
                    result.addChild(resultNode);
                    performCompare(resultNode,nextLHS,nextRHS);
                }
            }

            if(!added.contains(lhsName)) {
                added.add(lhsName);
                FileTreeNode resultNode = createCompareNode(CompareStatusType.REMOVED, result, nextLHS, nullNode);
                result.addChild(resultNode);
                performCompare(resultNode,nextLHS,nullNode);
            }
        }

        for(FileTreeNode nextRHS : rhs.getChildren()) {
            String rhsName = nextRHS.getName().orElse("");
            if(rhsName.length() == 0) {
                continue;
            }

            if(!added.contains(rhsName)) {
                added.add(rhsName);
                FileTreeNode resultNode = createCompareNode(CompareStatusType.ADDED, result, nullNode, nextRHS);
                result.addChild(resultNode);
                performCompare(resultNode,nullNode,nextRHS);
            }
        }
    }

    protected void internalCompare(FileTreeNode lhs, FileTreeNode rhs) {
        performCompare(this, lhs, rhs);
    }

    protected abstract void findDeleteFiles(FileTreeNode node, List<FileTreeNode> result);

    protected abstract void findDeleteDirectories(FileTreeNode node, List<FileTreeNode> result);

    protected abstract void findInsertDirectories(FileTreeNode node, List<FileTreeNode> result);

    protected abstract void findInsertFiles(FileTreeNode node, List<FileTreeNode> result);

    public List<FileTreeNode> getOrderedNodeList() {
        // Nodes are placed in this order in the list:
        //
        // DELETE file      - delete file details from DB (order not important).
        // DELETE directory - delete directory details from DB (highest level first).
        // INSERT directory - insert directory details (lowest level first).
        // INSERT file      - insert files (order not important).

        List<FileTreeNode> result = new ArrayList<>();

        // Get the nodes that represent a delete file.
        result.add(new SectionNode(SectionNode.SectionNodeType.FILE_FOR_REMOVE));
        findDeleteFiles(this, result);

        // Get the nodes that represent a delete directory.
        result.add(new SectionNode(SectionNode.SectionNodeType.DIRECTORY_FOR_REMOVE));
        findDeleteDirectories(this, result);

        // Get the nodes that represent an insert directory.
        result.add(new SectionNode(SectionNode.SectionNodeType.DIRECTORY_FOR_INSERT));
        findInsertDirectories(this, result);

        // Get the nodes that represent an insert file.
        result.add(new SectionNode(SectionNode.SectionNodeType.FILE_FOR_INSERT));
        findInsertFiles(this, result);
        return result;
    }
}
