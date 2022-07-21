package com.jbr.middletier.backup.filetree.helpers;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.RootFileTreeNode;

import java.util.Optional;

public class BasicTestRootNode extends RootFileTreeNode {
    @Override
    public Optional<String> getName() {
        return Optional.empty();
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
    }
}
