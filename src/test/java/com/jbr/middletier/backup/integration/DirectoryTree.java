package com.jbr.middletier.backup.integration;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class DirectoryTree {
    public class DirectoryTreeNode {
        public String name;
        public Integer id;
        public List<DirectoryTreeNode> leafs;

        public DirectoryTreeNode(String name) {
            this.name = name;
            this.leafs = new ArrayList<>();
            this.id = -1;
        }
    }

    DirectoryTreeNode level1;

    public DirectoryTree(List<String> paths) {
        level1 = new DirectoryTreeNode("");

        for(String nextPath: paths) {
            DirectoryTreeNode currentLevel = level1;

            String[] directories = nextPath.split("/");

            for(String nextDirectory: directories) {
                // Does this exist at the current level?
                boolean found = false;
                for(DirectoryTreeNode nextNode: currentLevel.leafs) {
                    if(nextNode.name.equals(nextDirectory)) {
                        currentLevel = nextNode;
                        found = true;
                    }
                }

                if(!found) {
                    DirectoryTreeNode newNode = new DirectoryTreeNode(nextDirectory);
                    currentLevel.leafs.add(newNode);
                    currentLevel = newNode;
                }
            }
        }
    }

    private void findDirectories(FileSystemObject source, DirectoryRepository dbDirectories, DirectoryTreeNode node) {
        // Load the directories from the database.
        for(DirectoryInfo directoryInfo: dbDirectories.findByParentId(source.getIdAndType().getId())) {
            DirectoryTreeNode newNode = new DirectoryTreeNode(directoryInfo.getName());
            newNode.id = directoryInfo.getIdAndType().getId();
            node.leafs.add(newNode);
            findDirectories(directoryInfo,dbDirectories,newNode);
        }
    }

    public DirectoryTree(Source source, DirectoryRepository dbDirectories) {
        level1 = new DirectoryTreeNode("");
        findDirectories(source, dbDirectories, level1);
    }

    private void AssertLevel(DirectoryTreeNode myNode, DirectoryTreeNode expectedNode) {
        Assert.assertEquals(expectedNode.name,myNode.name);

        for(DirectoryTreeNode nextLeaf: myNode.leafs) {
            boolean found = false;
            for(DirectoryTreeNode nextExpectedLeaf: expectedNode.leafs) {
                if(nextLeaf.name.equals(nextExpectedLeaf.name)) {
                    AssertLevel(nextLeaf, nextExpectedLeaf);
                    found = true;
                }
            }
            Assert.assertTrue(found);
        }
    }

    public void AssertExpected(DirectoryTree expected) {
        AssertLevel(level1, expected.level1);
    }

    private int FindDirectoryAtLevel(String[] elements, int level, DirectoryTreeNode levelNode) {
        for(DirectoryTreeNode nextNodeLevel: levelNode.leafs) {
            if(nextNodeLevel.name.equals(elements[level])) {
                if(elements.length - 1 == level) {
                    return nextNodeLevel.id;
                }

                return FindDirectoryAtLevel(elements,level + 1, nextNodeLevel);
            }
        }

        return -1;
    }

    public int FindDirectory(String path) {
        String[] elements = path.split("/");
        int index = 0;
        return FindDirectoryAtLevel(elements,0, level1);
    }
}
