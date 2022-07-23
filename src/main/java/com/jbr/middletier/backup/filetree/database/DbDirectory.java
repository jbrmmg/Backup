package com.jbr.middletier.backup.filetree.database;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.filetree.FileTreeNode;

import java.util.Optional;

public class DbDirectory extends DbNode {
    private final DirectoryInfo directoryInfo;

    public DbDirectory(FileTreeNode parent, DirectoryInfo directoryInfo, FileRepository fileRepository, DirectoryRepository directoryRepository) {
        super(parent);
        this.directoryInfo = directoryInfo;

        for(DirectoryInfo nextDirectory : directoryRepository.findByParentId(directoryInfo.getIdAndType().getId())) {
            addChild(new DbDirectory(this, nextDirectory, fileRepository, directoryRepository));
        }

        for(FileInfo nextFile : fileRepository.findByParentId(directoryInfo.getIdAndType().getId())) {
            addChild(new DbFile(this, nextFile));
        }
    }

    @Override
    public Optional<String> getName() {
        return Optional.of(directoryInfo.getName());
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        if(newChild instanceof DbDirectory || newChild instanceof DbFile) {
            return;
        }

        throw new IllegalStateException("Database Directory children must be Database Directory or File.");
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public FileSystemObjectId getObjectId() {
        return directoryInfo.getIdAndType();
    }

    @Override
    public DbNodeCompareResultType compare(DbNode rhs) {
        if(rhs == this)
            return DbNodeCompareResultType.DBC_EQUAL;

        if( !(rhs instanceof DbDirectory) )
            return DbNodeCompareResultType.DBC_NOT_EQUAL;

        DbDirectory lhs = (DbDirectory) rhs;

        // They are equal if the names match.
        return this.directoryInfo.getName().equals(lhs.directoryInfo.getName()) ? DbNodeCompareResultType.DBC_EQUAL : DbNodeCompareResultType.DBC_NOT_EQUAL;
    }

    @Override
    public FileSystemObject getFSO() {
        return this.directoryInfo;
    }
}
