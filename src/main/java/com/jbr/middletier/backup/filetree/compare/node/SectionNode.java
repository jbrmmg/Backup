package com.jbr.middletier.backup.filetree.compare.node;

import com.jbr.middletier.backup.filetree.FileTreeNode;

import java.util.Objects;
import java.util.Optional;

public class SectionNode extends FileTreeNode  {
    public enum SectionNodeType { FILE_FOR_REMOVE, DIRECTORY_FOR_REMOVE, DIRECTORY_FOR_INSERT, FILE_FOR_INSERT }

    private final SectionNodeType section;

    public SectionNode(SectionNodeType section) {
        super(null);
        this.section = Objects.requireNonNull(section,"Cannot initialise a Rw DB Section with null.");
    }

    public SectionNodeType getSection() {
        return this.section;
    }

    @Override
    public Optional<String> getName() {
        return Optional.empty();
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // This is not required.
    }
}
