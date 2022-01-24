package com.jbr.middletier.backup.filetree;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FileTreeNode {
    public enum CompareStatusType { UNKNOWN, EQUAL, CHANGE_TO_DIRECTORY, CHANGE_TO_FILE, ADDED, REMOVED, UPDATED }

    public static int INVALID_ID = -1;

    private final FileTreeNode parent;
    private final List<FileTreeNode> children;
    private final boolean directory;
    private int id;
    protected String name;
    protected CompareStatusType compareStatus;

    protected FileTreeNode(Path path, FileTreeNode parent) {
        this.children = new LinkedList<>();
        this.name = path.getFileName().toString();
        this.directory = path.toFile().isDirectory();
        this.compareStatus = CompareStatusType.UNKNOWN;
        this.parent = parent;
        this.id = INVALID_ID;
    }

    protected FileTreeNode(DirectoryInfo directory, FileTreeNode parent) {
        this.children = new LinkedList<>();
        this.name = directory.getName();
        this.directory = true;
        this.compareStatus = CompareStatusType.UNKNOWN;
        this.parent = parent;
        this.id = directory.getId();
    }

    protected FileTreeNode(FileInfo file, FileTreeNode parent) {
        this.children = new LinkedList<>();
        this.name = file.getName();
        this.directory = false;
        this.compareStatus = CompareStatusType.UNKNOWN;
        this.parent = parent;
        this.id = file.getId();
    }

    private void deepCopy(List<FileTreeNode> sourceList, CompareStatusType compareStatus) {
        for(FileTreeNode next: sourceList) {
            this.children.add(new FileTreeNode(next,true, compareStatus, this));
        }
    }

    protected FileTreeNode(FileTreeNode primarySource, FileTreeNode secondarySource, boolean deepCopy, FileTreeNode parent) {
        this.children = new LinkedList<>();
        this.name = primarySource.name;
        this.directory = primarySource.directory;
        this.compareStatus = CompareStatusType.UNKNOWN;
        this.parent = parent;
        this.id = primarySource.id != INVALID_ID ? primarySource.id : secondarySource.id;

        if(deepCopy) {
            deepCopy(primarySource.children, CompareStatusType.UNKNOWN);
        }
    }

    protected FileTreeNode(FileTreeNode sourceNode, boolean deepCopy, CompareStatusType compareStatus, FileTreeNode parent) {
        this.children = new LinkedList<>();
        this.name = sourceNode.name;
        this.directory = sourceNode.directory;
        this.compareStatus = compareStatus;
        this.parent = parent;
        this.id = sourceNode.id;

        if(deepCopy) {
            deepCopy(sourceNode.children, compareStatus);
        }
    }

    private FileTreeNode addChildInternal(FileTreeNode child) {
        // Children can only be added to a directory.
        if(!directory) {
            throw new IllegalStateException("Cannot add children to a file! > " + child.name);
        }

        this.children.add(child);
        return child;
    }

    @SuppressWarnings("UnusedReturnValue")
    public FileTreeNode addChild(Path path) {
        return addChildInternal(new FileTreeNode(path,this));
    }

    @SuppressWarnings("UnusedReturnValue")
    public FileTreeNode addChild(DirectoryInfo directory) {
        return addChildInternal(new FileTreeNode(directory, this));
    }

    @SuppressWarnings("UnusedReturnValue")
    public FileTreeNode addChild(FileInfo file) {
        return addChildInternal(new FileTreeNode(file, this));
    }

    @SuppressWarnings("UnusedReturnValue")
    public FileTreeNode addChild(FileTreeNode source, CompareStatusType compareStatus) {
        return addChildInternal(new FileTreeNode(source, true, compareStatus, this));
    }

    @SuppressWarnings("UnusedReturnValue")
    public FileTreeNode addChild(FileTreeNode source1, FileTreeNode source2) {
        return addChildInternal(new FileTreeNode(source1, source2, false, this));
    }

    public List<FileTreeNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public FileTreeNode getNamedChild(String name) {
        for(FileTreeNode next: children) {
            if(next.name.equals(name)) {
                return next;
            }
        }

        return null;
    }

    public boolean isDirectory() {
        return this.directory;
    }

    public FileTreeNode getParent() {
        return this.parent;
    }

    public int getId() {
        return this.id;
    }

    public boolean hasValidId() {
        return this.id != INVALID_ID;
    }

    public void setId(DirectoryInfo createdDirectory) {
        if(createdDirectory.getId() == null) {
            throw new IllegalStateException("created directory id is null");
        }

        this.id = createdDirectory.getId();
    }

    public CompareStatusType getCompareStatus() {
        return compareStatus;
    }

    public void setCompareStatus(CompareStatusType compareStatus) {
        this.compareStatus = compareStatus;
    }

    public String getName() {
        return this.name;
    }

    private static String addToPath(FileTreeNode node, String path) {
        if(node.parent != null) {
            path = addToPath(node.parent,path);
        }

        return path + "/" + node.name;
    }

    public Path getPath() {
        return new File(addToPath(this, "")).toPath();
    }

    protected void removeFilteredChildren(String filter) {
        List<FileTreeNode> ignore = new ArrayList<>();
        for(FileTreeNode next: getChildren()) {
            if(!next.getName().matches(filter)) {
                ignore.add(next);
            }
        }

        this.children.removeAll(ignore);
    }

    @Override
    public String toString() {
        return "Tree Node [" +
                this.name +
                "] (" +
                (this.parent == null ? "No Parent" : "Parent") +
                "," +
                this.id +
                "," +
                compareStatus.toString() +
                ")";
    }
}
