package com.jbr.middletier.backup.filetree;

public abstract class RootFileTreeNode extends FileTreeNode {

    protected RootFileTreeNode() {
        super(null);
    }

    @Override
    public String toString() {
        return "Root: " + this.children.size();
    }
}
