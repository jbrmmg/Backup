package com.jbr.middletier.backup.filetree;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FileTreeNode {
    public enum CompareStatusType { UNKNOWN, EQUAL, CHANGE_TO_DIRECTORY, CHANGE_TO_FILE, ADDED, REMOVED, UPDATED }

    private FileTreeNode parent;
    private List<FileTreeNode> children;
    private boolean directory;
    protected String name;
    protected int id;
    protected CompareStatusType compareStatus;

    protected FileTreeNode(Path path) {
        this.children = new LinkedList<>();
        this.name = path.getFileName().toString();
        this.directory = path.toFile().isDirectory();
        this.id = -1;
        this.compareStatus = CompareStatusType.UNKNOWN;
    }

    protected FileTreeNode(DirectoryInfo directory) {
        this.children = new LinkedList<>();
        this.name = directory.getName();
        this.directory = true;
        this.id = directory.getId();
        this.compareStatus = CompareStatusType.UNKNOWN;
    }

    protected FileTreeNode(FileInfo file) {
        this.children = new LinkedList<>();
        this.name = file.getName();
        this.directory = false;
        this.id = file.getId();
        this.compareStatus = CompareStatusType.UNKNOWN;
    }

    protected FileTreeNode(FileTreeNode node, boolean deepCopy) {
        if(deepCopy) {
            this.children = new LinkedList<>();
        } else {
            this.children = node.children;
        }
        this.name = node.name;
        this.directory = node.directory;
        this.id = node.id;
        this.compareStatus = CompareStatusType.UNKNOWN;
    }

    protected FileTreeNode addChild(FileTreeNode child) {
        // Children can only be added to a directory.
        if(!directory) {
            throw new IllegalStateException("Cannot add children to a file! > " + child.name);
        }

        child.parent = this;
        this.children.add(child);
        return child;
    }

    public FileTreeNode addChild(Path path) {
        return addChild(new FileTreeNode(path));
    }

    public FileTreeNode addChild(DirectoryInfo directory) {
        return addChild(new FileTreeNode(directory));
    }

    public FileTreeNode addChild(FileInfo file) {
        return addChild(new FileTreeNode(file));
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

    public CompareStatusType getCompareStatus() {
        return compareStatus;
    }

    public void setCompareStatus(CompareStatusType compareStatus) {
        this.compareStatus = compareStatus;
    }

    public boolean removing() {
        return this.compareStatus == CompareStatusType.REMOVED || this.compareStatus == CompareStatusType.CHANGE_TO_FILE;
    }

    public boolean adding() {
        return this.compareStatus == CompareStatusType.ADDED || this.compareStatus == CompareStatusType.CHANGE_TO_DIRECTORY;
    }
}
