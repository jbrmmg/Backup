package com.jbr.middletier.backup.filetree;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FileTreeNode {
    public enum CompareStatusType { UNKNOWN, EQUAL, CHANGE_TO_DIRECTORY, CHANGE_TO_FILE, ADDED, REMOVED, UPDATED }

    private final FileTreeNode parent;
    private final List<FileTreeNode> children;
    private final boolean directory;
    private final Path sourcePath;
    private final DirectoryInfo sourceDbDirectory;
    private final FileInfo sourceDbFile;
    protected String name;
    protected CompareStatusType compareStatus;
    private DirectoryInfo createdDirectory;

    protected FileTreeNode(Path path, FileTreeNode parent) {
        this.children = new LinkedList<>();
        this.name = path.getFileName().toString();
        this.directory = path.toFile().isDirectory();
        this.compareStatus = CompareStatusType.UNKNOWN;
        this.parent = parent;
        this.sourcePath = path;
        this.sourceDbDirectory = null;
        this.sourceDbFile = null;
        this.createdDirectory = null;
    }

    protected FileTreeNode(DirectoryInfo directory, FileTreeNode parent) {
        this.children = new LinkedList<>();
        this.name = directory.getName();
        this.directory = true;
        this.compareStatus = CompareStatusType.UNKNOWN;
        this.parent = parent;
        this.sourcePath = null;
        this.sourceDbDirectory = directory;
        this.sourceDbFile = null;
        this.createdDirectory = null;
    }

    protected FileTreeNode(FileInfo file, FileTreeNode parent) {
        this.children = new LinkedList<>();
        this.name = file.getName();
        this.directory = false;
        this.compareStatus = CompareStatusType.UNKNOWN;
        this.parent = parent;
        this.sourcePath = null;
        this.sourceDbDirectory = null;
        this.sourceDbFile = file;
        this.createdDirectory = null;
    }

    protected FileTreeNode(FileTreeNode node1, FileTreeNode node2, boolean deepCopy, FileTreeNode parent) {
        if(deepCopy) {
            this.children = node1.children;
        } else {
            this.children = new LinkedList<>();
        }
        this.name = node1.name;
        this.directory = node1.directory;
        this.compareStatus = CompareStatusType.UNKNOWN;
        this.parent = parent;

        this.sourcePath = (node1.sourcePath == null) ? node2.sourcePath : node1.sourcePath;
        this.sourceDbDirectory = (node1.sourceDbDirectory == null) ? node2.sourceDbDirectory : node1.sourceDbDirectory;
        this.sourceDbFile = (node1.sourceDbFile == null) ? node2.sourceDbFile : node1.sourceDbFile;
        this.createdDirectory = null;
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
    public FileTreeNode addChild(FileTreeNode source) {
        return addChildInternal(new FileTreeNode(source, null, true, this));
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

    public CompareStatusType getCompareStatus() {
        return compareStatus;
    }

    public void setCompareStatus(CompareStatusType compareStatus) {
        this.compareStatus = compareStatus;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public DirectoryInfo getSourceDbDirectory() {
        return sourceDbDirectory;
    }

    public FileInfo getSourceDbFile() {
        return sourceDbFile;
    }

    public String getName() {
        return this.name;
    }

    public DirectoryInfo getCreatedDirectory() {
        return createdDirectory;
    }

    public void setCreatedDirectory(DirectoryInfo createdDirectory) {
        this.createdDirectory = createdDirectory;
    }

    @Override
    public String toString() {
        return "Tree Node [" +
                this.name +
                "] (" +
                (this.parent == null ? "No Parent" : "Parent") +
                "," +
                (this.sourcePath == null ? "No Path" : this.sourcePath.toString()) +
                "," +
                (this.sourceDbFile == null ? "No DB File" : this.sourceDbFile.getId()) +
                "," +
                (this.sourceDbDirectory == null ? "No DB Directory" : this.sourceDbDirectory.getId()) +
                "," +
                compareStatus.toString() +
                ")";
    }
}
