package com.jbr.middletier.backup.filetree.helpers;

import com.jbr.middletier.backup.filetree.realworld.RwDirectory;
import org.junit.Assert;

import java.io.File;
import java.util.Optional;

public class BasicRwDirectory extends RwDirectory {
    public BasicRwDirectory() {
        super(null, new File("test").toPath());
    }

    @Override
    public Optional<String> getName() {
        return Optional.of("Test");
    }

    public boolean test() {
        childAdded(new BasicRwDirectory());
        childAdded(new BasicRwFile());
        BasicTestNode testNode = new BasicTestNode();
        try {
            childAdded(testNode);
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertEquals("Real World Directory children must be Real World Directory or File.",e.getMessage());
            return true;
        }

        return false;
    }
}
