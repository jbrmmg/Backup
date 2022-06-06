package com.jbr.middletier.backup.filetree.database;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.filetree.FileTreeNode;

public class DbDirectory extends DbNode {
    private DirectoryInfo directoryInfo;

    public DbDirectory(FileTreeNode parent, DirectoryInfo directoryInfo, FileRepository fileRepository, DirectoryRepository directoryRepository) {
        super(parent);
        this.directoryInfo = directoryInfo;

        for(DirectoryInfo nextDirectory : directoryRepository.findByParentId(directoryInfo.getIdAndType().getId())) {
            children.add(new DbDirectory(this, nextDirectory, fileRepository, directoryRepository));
        }

        for(FileInfo nextFile : fileRepository.findByParentId(directoryInfo.getIdAndType().getId())) {
            children.add(new DbFile(this, nextFile));
        }
    }

    @Override
    public String getName() {
        return directoryInfo.getName();
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
}
