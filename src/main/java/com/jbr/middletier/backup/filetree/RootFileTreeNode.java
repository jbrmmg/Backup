package com.jbr.middletier.backup.filetree;

import com.jbr.middletier.backup.data.Source;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class RootFileTreeNode extends FileTreeNode {

    protected RootFileTreeNode() {
        super(null);
    }

    protected List<FileTreeNode> getChildren(FileTreeNode parent) {
        return parent.children;
    }
}
