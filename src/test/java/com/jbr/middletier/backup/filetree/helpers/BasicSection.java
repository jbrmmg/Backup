package com.jbr.middletier.backup.filetree.helpers;

import com.jbr.middletier.backup.filetree.compare.node.SectionNode;

public class BasicSection extends SectionNode {
    public BasicSection(SectionNodeType section) {
        super(section);
    }

    public void test() {
        childAdded(null);
    }
}
