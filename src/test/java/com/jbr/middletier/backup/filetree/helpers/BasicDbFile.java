package com.jbr.middletier.backup.filetree.helpers;

import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.filetree.database.DbFile;
import org.junit.Assert;

public class BasicDbFile extends DbFile {
    public BasicDbFile() {
        super(null, new FileInfo());
    }

    public boolean test() {
        try {
            childAdded(null);
            Assert.fail();
            return false;
        } catch (IllegalStateException e) {
            Assert.assertEquals("Cannot add child nodes to a file database node.", e.getMessage());
        }

        return true;
    }
}
