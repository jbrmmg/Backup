package com.jbr.middletier.backup.filetree.realworld;

import com.jbr.middletier.backup.filetree.FileTreeNode;

import java.nio.file.Path;
import java.util.Optional;

public class RwDirectory extends RwNode {
    private final Path path;

    public RwDirectory(FileTreeNode parent, Path path) {
        super(parent);
        this.path = path;
    }

    @Override
    public Optional<String> getName() {
        return Optional.of(path.getFileName().toString());
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
        return "RW (dir): " + getName().orElse("") + " " + this.children.size();
    }
}
