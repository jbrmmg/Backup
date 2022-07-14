package com.jbr.middletier.backup.filetree.helpers;

import com.jbr.middletier.backup.filetree.compare.node.SectionNode;

public class BasicSection extends SectionNode {
    public BasicSection(SectionNodeType section) {
        super(section);
    }

    public boolean test() {
        childAdded(null);
        return true;
    }
}
