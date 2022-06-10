package com.jbr.middletier.backup.filetree.compare.node;

import com.jbr.middletier.backup.filetree.FileTreeNode;

public class RwDbSectionNode extends FileTreeNode  {
    public enum RwDbSectionNodeType { FILE_FOR_REMOVE, DIRECTORY_FOR_REMOVE, DIRECTORY_FOR_INSERT, FILE_FOR_INSERT };

    private RwDbSectionNodeType section;

    public RwDbSectionNode(RwDbSectionNodeType section) {
        super(null);
        this.section = section;
    }

    public RwDbSectionNodeType getSection() {
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
