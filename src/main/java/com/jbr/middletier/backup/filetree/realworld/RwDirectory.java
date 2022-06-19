package com.jbr.middletier.backup.filetree.realworld;

import com.jbr.middletier.backup.filetree.FileTreeNode;

import java.nio.file.Path;

public class RwDirectory extends RwNode {
    private final Path path;

    public RwDirectory(FileTreeNode parent, Path path) {
        super(parent);
        this.path = path;
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        if(newChild instanceof RwFile || newChild instanceof RwDirectory) {
            return;
        }

        throw new IllegalStateException("Real World Directory children must be Real World Directory or File.");
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public String toString() {
        return "RW (dir): " + getName() + " " + this.children.size();
    }
}
