package com.jbr.middletier.backup.filetree.realworld;

import com.jbr.middletier.backup.filetree.FileTreeNode;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class RwFile extends RwNode {
    private final File file;

    public RwFile(FileTreeNode parent, Path path) {
        super(parent);
        file = path.toFile();
    }

    @Override
    public Optional<String> getName() {
        return Optional.of(file.getName());
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // This is not allow.
        throw new IllegalStateException("Cannot add child nodes to a file node.");
    }

    public File getFile() {
        return this.file;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public String toString() {
        return "RW (file): " + getName().orElse("");
    }
}
