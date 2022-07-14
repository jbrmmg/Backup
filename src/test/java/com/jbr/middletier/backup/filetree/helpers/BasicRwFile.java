package com.jbr.middletier.backup.filetree.helpers;

import com.jbr.middletier.backup.filetree.realworld.RwFile;
import org.junit.Assert;

import java.io.File;

public class BasicRwFile extends RwFile {
    public BasicRwFile() {
        super(null, new File("test").toPath());
    }

    public boolean test() {
        try {
            childAdded(null);
            Assert.fail();
        } catch(IllegalStateException e) {
            Assert.assertEquals("Cannot add child nodes to a file node.", e.getMessage());
        }

        return true;
    }

    @Override
    public String getName() {
        return "test";
    }
}
