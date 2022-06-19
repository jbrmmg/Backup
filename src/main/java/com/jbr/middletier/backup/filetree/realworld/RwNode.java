package com.jbr.middletier.backup.filetree.realworld;

import com.jbr.middletier.backup.filetree.FileTreeNode;

public abstract class RwNode extends FileTreeNode {
    protected RwNode(FileTreeNode parent) {
        super(parent);
    }

    public abstract boolean isDirectory();
}
