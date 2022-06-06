package com.jbr.middletier.backup.filetree.database;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;

public class DbRoot extends RootFileTreeNode {
    private Source databaseSource;

    public DbRoot(Source databaseSource, FileRepository fileRepository, DirectoryRepository directoryRepository) {
        this.databaseSource = databaseSource;

        for(DirectoryInfo nextDirectory : directoryRepository.findByParentId(databaseSource.getIdAndType().getId())) {
            children.add(new DbDirectory(this,nextDirectory,fileRepository,directoryRepository));
        }
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // Nothing required.
    }
}
