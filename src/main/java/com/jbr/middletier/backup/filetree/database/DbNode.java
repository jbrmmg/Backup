package com.jbr.middletier.backup.filetree.database;

import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.filetree.FileTreeNode;

public abstract class DbNode extends FileTreeNode {
    protected DbNode(FileTreeNode parent) {
        super(parent);
    }

    public abstract boolean isDirectory();

    public abstract FileSystemObjectId getObjectId();

    public abstract DbNodeCompareResultType compare(DbNode rhs);
}
