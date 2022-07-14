package com.jbr.middletier.backup.filetree.realworld;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;
import com.jbr.middletier.backup.manager.FileSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RwRoot extends RootFileTreeNode {
    private final Path rootOfRealWorld;

    public RwRoot(String pathName, FileSystem fileSystem) throws IOException {
        this.rootOfRealWorld = new File(pathName).toPath();

        fileSystem.walkThePath(this.rootOfRealWorld, path -> {
            if (path.getNameCount() > this.rootOfRealWorld.getNameCount()) {
                FileTreeNode nextIterator = this;

                for (int directoryIdx = rootOfRealWorld.getNameCount(); directoryIdx < path.getNameCount() - 1; directoryIdx++) {
                    nextIterator = nextIterator.getNamedChild(path.getName(directoryIdx).toString());
                }

                // Is this a directory?
                if (fileSystem.isDirectory(path)) {
                    nextIterator.addChild(new RwDirectory(nextIterator, path));
                } else {
                    nextIterator.addChild(new RwFile(nextIterator, path));
                }
            }
        });
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // Nothing required.
    }

    public void removeFilteredChildren(String filter) {
        // Remove anything from realworld that does not meet the source filter.
        if(filter != null && filter.length() > 0) {
            List<FileTreeNode> toBeRemoved = new ArrayList<>();

            for(FileTreeNode nextNode : this.children) {
                if(!nextNode.getName().matches(filter)) {
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
