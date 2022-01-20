package com.jbr.middletier.backup.filetree;

import com.jbr.middletier.backup.data.Source;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RootFileTreeNode extends FileTreeNode {
    private String rootDirectory;

    public RootFileTreeNode(Path path) {
        super(path);
        this.rootDirectory = path.getParent().toString();
    }

    public RootFileTreeNode(Source source) {
        super(Paths.get(source.getPath()));
        this.rootDirectory = Paths.get(source.getPath()).getParent().toString();
        this.id = source.getId();
    }

    public RootFileTreeNode(RootFileTreeNode node) {
        super(node, false);
        this.rootDirectory = node.rootDirectory;
        this.id = node.id;
    }

    private long getCount(FileTreeNode node) {
        long result = node.getChildren().size();

        for(FileTreeNode next: node.getChildren()) {
            result += getCount(next);
        }

        return result;
    }

    public long getFileCount() {
        return getCount(this);
    }

    public void compare(FileTreeNode result, FileTreeNode rhs) {
        // Compare the children.
        for(FileTreeNode next: getChildren()) {
            // Does the rhs have this child?
            FileTreeNode rhsChild = rhs.getNamedChild(next.name);

            if(rhsChild == null) {
                // Create an entry in the result (deep copy)
                result.addChild(new FileTreeNode(next,true)).compareStatus = CompareStatusType.ADDED;
            } else {
                // Create a copy in the result and then process the children.
                FileTreeNode resultChild = result.addChild(new FileTreeNode(rhsChild, false));

                // Set the status
                if (next.isDirectory() == rhs.isDirectory()) {
                    // Both are the same.
                    resultChild.compareStatus = CompareStatusType.EQUAL;
                } else if(next.isDirectory()) {
                    // Gone from file to directory
                    resultChild.compareStatus = CompareStatusType.CHANGE_TO_DIRECTORY;
                } else {
                    // Gone from directory to file
                    resultChild.compareStatus = CompareStatusType.CHANGE_TO_FILE;
                }

                // Process the children
                compare(resultChild, rhsChild);
            }
        }

        // Check for children not on the rhs
        for(FileTreeNode next: rhs.getChildren()) {
            FileTreeNode lhsChild = this.getNamedChild(next.name);

            if(lhsChild == null) {
                // Create an entry on the result.
                result.addChild(new FileTreeNode(next,true)).compareStatus = CompareStatusType.REMOVED;
            }
        }
    }

    public RootFileTreeNode compare(RootFileTreeNode rhs) {
        // Firstly the two roots must be for the same location.
        if(!this.rootDirectory.equals(rhs.rootDirectory)) {
            throw new IllegalStateException("Compare RootFileTreeNode must be for the same root.");
        } else if(!this.name.equals(rhs.name)) {
            throw new IllegalStateException("Compare RootFileTreeNode must be for the same root and name.");
        }

        RootFileTreeNode result = new RootFileTreeNode(this);
        result.compareStatus = CompareStatusType.EQUAL;
        compare(result,rhs);
        return result;
    }
}
