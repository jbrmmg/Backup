package com.jbr.middletier.backup.filetree;

import java.util.List;

public abstract class RootFileTreeNode extends FileTreeNode {

    protected RootFileTreeNode() {
        super(null);
    }

//    protected List<FileTreeNode> getChildren(FileTreeNode parent) {
//        return parent.children;
//    }

    @Override
    public String toString() {
        return "Root: " + this.children.size();
    }
}
