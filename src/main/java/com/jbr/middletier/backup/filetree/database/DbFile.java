package com.jbr.middletier.backup.filetree.database;

import com.jbr.middletier.backup.data.Classification;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.filetree.FileTreeNode;

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
    public String getName() {
        return fileInfo.getName();
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
    public DbNodeCompareResultType compare(DbNode rhs) {
        if(rhs == this)
            return DbNodeCompareResultType.DBC_NOT_EQUAL;

        if( !(rhs instanceof DbFile) )
            return DbNodeCompareResultType.DBC_NOT_EQUAL;

        DbFile rhsFile = (DbFile) rhs;

        // They are equal if the names match, date, size and if available the MD5.
        if (!this.fileInfo.getName().equals(rhsFile.fileInfo.getName()))
            return DbNodeCompareResultType.DBC_NOT_EQUAL;

        if(!this.fileInfo.getSize().equals(rhsFile.fileInfo.getSize()))
            return DbNodeCompareResultType.DBC_NOT_EQUAL;

        if(!this.fileInfo.getDate().equals(rhsFile.fileInfo.getDate())) {
            if(this.fileInfo.getMD5().equals(rhsFile.fileInfo.getMD5())) {
                return DbNodeCompareResultType.DBC_EQUAL_EXCEPT_DATE;
            } else {
                return DbNodeCompareResultType.DBC_NOT_EQUAL;
            }
        }

        return this.fileInfo.getMD5().equals(rhsFile.fileInfo.getMD5()) ? DbNodeCompareResultType.DBC_EQUAL : DbNodeCompareResultType.DBC_NOT_EQUAL;
    }
}
