package com.jbr.middletier.backup.filetree.database;

import com.jbr.middletier.backup.data.Classification;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import java.util.Optional;

public class DbFile extends DbNode {
    private final FileInfo fileInfo;

    public DbFile(FileTreeNode parent, FileInfo fileInfo) {
        super(parent);
        this.fileInfo = fileInfo;
    }

    public Classification getClassification() {
        return fileInfo.getClassification();
    }

    @Override
    public Optional<String> getName() {
        return Optional.of(fileInfo.getName());
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // This is not allow.
        throw new IllegalStateException("Cannot add child nodes to a file database node.");
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public FileSystemObjectId getObjectId() {
        return fileInfo.getIdAndType();
    }

    @Override
    public boolean compare(DbNode rhs) {
        if(rhs == this)
            return true;

        if( !(rhs instanceof DbFile rhsFile) )
            return false;

        // They are equal if the names match, size and MD5.
        if (!this.fileInfo.getName().equals(rhsFile.fileInfo.getName()))
            return false;

        if(!this.fileInfo.getSize().equals(rhsFile.fileInfo.getSize()))
            return false;

        return this.fileInfo.getMd5().equals(rhsFile.fileInfo.getMd5());
    }

    @Override
    public FileSystemObject getFSO() {
        return this.fileInfo;
    }
}
