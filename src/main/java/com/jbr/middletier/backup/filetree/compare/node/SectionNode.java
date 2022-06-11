package com.jbr.middletier.backup.filetree.compare.node;

import com.jbr.middletier.backup.filetree.FileTreeNode;

public class SectionNode extends FileTreeNode  {
    public enum SectionNodeType { UNKNOWN, FILE_FOR_REMOVE, DIRECTORY_FOR_REMOVE, DIRECTORY_FOR_INSERT, FILE_FOR_INSERT }

    private final SectionNodeType section;

    public SectionNode(SectionNodeType section) {
        super(null);
        this.section = section;

        if(section.equals(SectionNodeType.UNKNOWN)) {
            throw new IllegalStateException("Cannot initialise a Rw DB Section object as unknown.");
        }
    }

    public SectionNodeType getSection() {
        return this.section;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // This is not required.
    }
}
