package com.jbr.middletier.backup.filetree.helpers;

import com.jbr.middletier.backup.filetree.realworld.RwFile;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Optional;

public class BasicRwFile extends RwFile {
    public BasicRwFile() {
        super(null, new File("test").toPath());
    }

    public boolean test() {
        try {
            childAdded(null);
            fail();
        } catch(IllegalStateException e) {
            assertEquals("Cannot add child nodes to a file node.", e.getMessage());
            return true;
        }

        return false;
    }

    @Override
    public Optional<String> getName() {
        return Optional.of("test");
    }
}
