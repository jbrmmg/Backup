package com.jbr.middletier.backup.filetree.realworld;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;
import com.jbr.middletier.backup.manager.FileSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RwRoot extends RootFileTreeNode {
    private final Path rootOfRealWorld;

    public RwRoot(String pathName, FileSystem fileSystem) throws IOException {
        this.rootOfRealWorld = new File(pathName).toPath();

        fileSystem.walkThePath(this.rootOfRealWorld, path -> {
            if (path.getNameCount() <= this.rootOfRealWorld.getNameCount())
                return;

            Optional<FileTreeNode> nextIterator = Optional.of(this);

            for (int directoryIdx = rootOfRealWorld.getNameCount(); directoryIdx < path.getNameCount() - 1; directoryIdx++) {
                nextIterator = nextIterator.get().getNamedChild(path.getName(directoryIdx).toString());

                if(nextIterator.isEmpty())
                    return;
            }

            // Is this a directory?
            if (fileSystem.isDirectory(path)) {
                // Ignore directories that start with .
                if (!path.getFileName().toString().startsWith(".")) {
                    nextIterator.get().addChild(new RwDirectory(nextIterator.get(), path));
                }

                return;
            }

            nextIterator.get().addChild(new RwFile(nextIterator.get(), path));
        });
    }

    @Override
    public Optional<String> getName() {
        return Optional.empty();
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // Nothing required.
    }

    public void removeFilteredChildren(String filter) {
        // Remove anything from realworld that does not meet the source filter.
        if(filter != null && !filter.isEmpty()) {
            List<FileTreeNode> toBeRemoved = new ArrayList<>();

            for(FileTreeNode nextNode : this.children) {
                if(!nextNode.getName().orElse("").matches(filter)) {
                    toBeRemoved.add(nextNode);
                }
            }

            this.children.removeAll(toBeRemoved);
        }
    }

    @Override
    public String toString() {
        return "Real World (R): " + rootOfRealWorld.toString() + " " + this.children.size();
    }
}
