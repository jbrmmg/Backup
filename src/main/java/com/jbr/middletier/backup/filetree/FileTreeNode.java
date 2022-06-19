package com.jbr.middletier.backup.filetree;

import java.util.LinkedList;
import java.util.List;

public abstract class FileTreeNode {
    public enum CompareStatusType { EQUAL, ADDED, REMOVED, UPDATED }

    // Dummy node just used to represent a missing node.
    protected static FileTreeNode nullNode = new FileTreeNode(null) {
        @Override
        public String getName() {
            return null;
        }

        @Override
        protected void childAdded(FileTreeNode newChild) {
            // Cannot add children to this node.
            throw new IllegalStateException("Null node - cannot add children");
        }
    };

    private final FileTreeNode parent;
    protected final List<FileTreeNode> children;
    protected CompareStatusType status;

    protected FileTreeNode(FileTreeNode parent) {
        this.children = new LinkedList<>();
        this.status = CompareStatusType.EQUAL;
        this.parent = parent;
    }

    public FileTreeNode getParent() {
        return this.parent;
    }

    public abstract String getName();

    protected abstract void childAdded(FileTreeNode newChild);

    public CompareStatusType getStatus() {
        // If there are no children then use my own status.
        if(children.isEmpty()) {
            return this.status;
        }

        // Determine the status from the children.
        for(FileTreeNode next: children) {
            if(next.status != CompareStatusType.EQUAL) {
                return CompareStatusType.UPDATED;
            }
        }

        return CompareStatusType.EQUAL;
    }

    public FileTreeNode getNamedChild(String name) {
        for(FileTreeNode nextNode: this.children) {
            if(name.equals(nextNode.getName())) {
                return nextNode;
            }
        }

        return null;
    }

    public void addChild(FileTreeNode newChild) {
        childAdded(newChild);
        this.children.add(newChild);
    }

    public Iterable<FileTreeNode> getChildren() {
        return this.children;
    }

    @Override
    public String toString() {
        return getName() + " " + this.children.size();
    }
}
