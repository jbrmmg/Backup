package com.jbr.middletier.backup.filetree.realworld;

import com.jbr.middletier.backup.filetree.FileTreeNode;

import java.io.File;
import java.nio.file.Path;

public class RwFile extends RwNode {
    private File file;

    public RwFile(FileTreeNode parent, Path path) {
        super(parent);
        file = path.toFile();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // This is not allow.
        throw new IllegalStateException("Cannot add child notes to a file node.");
    }

    @Override
    public boolean isDirectory() {
        return false;
    }
}
