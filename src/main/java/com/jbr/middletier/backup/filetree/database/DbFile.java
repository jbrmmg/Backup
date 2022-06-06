package com.jbr.middletier.backup.filetree.database;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.filetree.FileTreeNode;

public class DbFile extends DbNode {
    private FileInfo fileInfo;

    public DbFile(FileTreeNode parent, FileInfo fileInfo) {
        super(parent);
        this.fileInfo = fileInfo;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // This is not allow.
        throw new IllegalStateException("Cannot add child notes to a file database node.");
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public FileSystemObjectId getObjectId() {
        return fileInfo.getIdAndType();
    }
}
