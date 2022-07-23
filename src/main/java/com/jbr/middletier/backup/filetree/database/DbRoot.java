package com.jbr.middletier.backup.filetree.database;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;

import java.util.Optional;

public class DbRoot extends RootFileTreeNode {
    private final Source databaseSource;

    public DbRoot(Source databaseSource, FileRepository fileRepository, DirectoryRepository directoryRepository) {
        this.databaseSource = databaseSource;

        for(DirectoryInfo nextDirectory : directoryRepository.findByParentId(databaseSource.getIdAndType().getId())) {
            addChild(new DbDirectory(this, nextDirectory, fileRepository, directoryRepository));
        }

        for(FileInfo nextFile : fileRepository.findByParentId(databaseSource.getIdAndType().getId())) {
            addChild(new DbFile(this, nextFile));
        }
    }

    public Source getSource() {
        return this.databaseSource;
    }

    @Override
    public Optional<String> getName() {
        return Optional.empty();
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // Nothing required.
    }
}
