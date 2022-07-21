package com.jbr.middletier.backup.filetree.helpers;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import org.junit.Assert;

import java.util.Optional;

public class BasicTestNode extends FileTreeNode {
    private String name;

    public BasicTestNode() {
        super(null);
        this.status = CompareStatusType.ADDED;
        this.name = null;
    }

    @Override
    public Optional<String> getName() {
        return this.name == null ? Optional.empty() : Optional.of(this.name);
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
    }

    public boolean test() {
        // Test the null node.
        Assert.assertNull(nullNode.getName());

        try {
            nullNode.addChild(null);
            Assert.fail();
            return false;
        } catch (IllegalStateException e) {
            Assert.assertEquals("Null node - cannot add children", e.getMessage());
        }

        return true;
    }

    public void test2() {
        BasicTestNode testNode = new BasicTestNode();
        testNode.status = CompareStatusType.ADDED;
        this.children.add(testNode);
    }

    public void test3() {
        BasicTestNode testNode = new BasicTestNode();
        testNode.status = CompareStatusType.EQUAL;
        this.children.clear();
        this.children.add(testNode);
    }

    public void test4() {
        BasicTestNode testNode = new BasicTestNode();
        testNode.status = CompareStatusType.EQUAL;
        testNode.name = "Hello";
        this.children.clear();
        this.children.add(testNode);
    }
}
