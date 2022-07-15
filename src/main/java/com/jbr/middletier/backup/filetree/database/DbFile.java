package com.jbr.middletier.backup.filetree.database;

import com.jbr.middletier.backup.data.Classification;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.filetree.FileTreeNode;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    private static boolean datesDiffer(LocalDateTime lhs, LocalDateTime rhs) {
        return Math.abs(ChronoUnit.SECONDS.between(lhs,rhs)) > 5;
    }

    @Override
    public DbNodeCompareResultType compare(DbNode rhs) {
        if(rhs == this)
            return DbNodeCompareResultType.DBC_EQUAL;

        if( !(rhs instanceof DbFile) )
            return DbNodeCompareResultType.DBC_NOT_EQUAL;

        DbFile rhsFile = (DbFile) rhs;

        // They are equal if the names match, date, size and if available the MD5.
        if (!this.fileInfo.getName().equals(rhsFile.fileInfo.getName()))
            return DbNodeCompareResultType.DBC_NOT_EQUAL;

        if(!this.fileInfo.getSize().equals(rhsFile.fileInfo.getSize()))
            return DbNodeCompareResultType.DBC_NOT_EQUAL;

        if(datesDiffer(this.fileInfo.getDate(),rhsFile.fileInfo.getDate())) {
            if(this.fileInfo.getMD5().compare(rhsFile.fileInfo.getMD5(),false)) {
                return DbNodeCompareResultType.DBC_EQUAL_EXCEPT_DATE;
            } else {
                return DbNodeCompareResultType.DBC_NOT_EQUAL;
            }
        }

        return this.fileInfo.getMD5().compare(rhsFile.fileInfo.getMD5(), false) ? DbNodeCompareResultType.DBC_EQUAL : DbNodeCompareResultType.DBC_NOT_EQUAL;
    }

    @Override
    public FileSystemObject getFSO() {
        return this.fileInfo;
    }
}
