package com.jbr.middletier.backup.filetree.compare.node;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import lombok.Getter;

import java.util.Objects;
import java.util.Optional;

@Getter
public class SectionNode extends FileTreeNode  {
    public enum SectionNodeType { FILE_FOR_REMOVE, DIRECTORY_FOR_REMOVE, DIRECTORY_FOR_INSERT, FILE_FOR_INSERT }

    private final SectionNodeType section;

    public SectionNode(SectionNodeType section) {
        super(null);
        this.section = Objects.requireNonNull(section,"Cannot initialise a Rw DB Section with null.");
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
