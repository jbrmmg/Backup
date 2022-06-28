package com.jbr.middletier.backup.filetree.helpers;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.compare.DbTree;
import com.jbr.middletier.backup.filetree.compare.node.DbCompareNode;
import com.jbr.middletier.backup.filetree.database.DbDirectory;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BasicDbTree extends DbTree {
    public BasicDbTree() {
        super(null, null);
    }

    public boolean testDirectoryRemoved() {
        DbDirectory mockDbDirectory = mock(DbDirectory.class);
        when(mockDbDirectory.isDirectory()).thenReturn(true);

        DbCompareNode test = (DbCompareNode)createCompareNode(CompareStatusType.REMOVED, null, mockDbDirectory, null);
        return test.getActionType().equals(DbCompareNode.ActionType.COPY) && test.getSubActionType().equals(DbCompareNode.SubActionType.NONE);
    }

    public boolean testRemovedNoSourceFailure() {
        try {
            createCompareNode(CompareStatusType.REMOVED, null, null, null);
            return false;
        } catch(IllegalStateException e) {
            Assert.assertEquals("Status is removed, but no source provided", e.getMessage());
        }

        return true;
    }

    public boolean testDirectoryAdded() {
        DbDirectory mockDbDirectory = mock(DbDirectory.class);
        when(mockDbDirectory.isDirectory()).thenReturn(true);

        DbCompareNode test = (DbCompareNode)createCompareNode(CompareStatusType.ADDED, null, null, mockDbDirectory);
        return test.getActionType().equals(DbCompareNode.ActionType.REMOVE) && test.getSubActionType().equals(DbCompareNode.SubActionType.NONE);
    }

    public boolean testAddedNoDestinationFailure() {
        try {
            createCompareNode(CompareStatusType.ADDED, null, null, null);
            Assert.fail();
            return false;
        } catch(IllegalStateException e) {
            Assert.assertEquals("Status is added, but no destination provided", e.getMessage());
        }

        return true;
    }

    private boolean validateDeleteFile(DbCompareNode node, int expectedCount) {
        List<FileTreeNode> list = new ArrayList<>();
        findDeleteFiles(node, list);

        return expectedCount == list.size();
    }

    public boolean deleteFileRemoveFile() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(false);

        return validateDeleteFile(node,1);
    }

    public boolean deleteFileRecreateAsDirectory() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY);
        when(node.isDirectory()).thenReturn(true);

        return validateDeleteFile(node,1);
    }

    public boolean deleteFileRemoveDirectory() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(true);

        return validateDeleteFile(node, 0);
    }

    public boolean deleteFileCopyFile() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(false);

        return validateDeleteFile(node, 0);
    }

    private boolean validateDeleteDirectories(DbCompareNode node, int expectedCount) {
        List<FileTreeNode> list = new ArrayList<>();
        findDeleteDirectories(node, list);

        return expectedCount == list.size();
    }

    public boolean deleteDirectoryRemoveDirectory() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(true);

        return validateDeleteDirectories(node, 1);
    }

    public boolean deleteDirectoryCopyDirectory() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(true);

        return validateDeleteDirectories(node, 0);
    }

    public boolean deleteDirectoryRemoveFile() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(false);

        return validateDeleteDirectories(node, 0);
    }

    public boolean deleteDirectoryRecreateAsFile() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_FILE);
        when(node.isDirectory()).thenReturn(false);

        return validateDeleteDirectories(node, 1);
    }

    private boolean validateInsertDirectories(DbCompareNode node, int expectedCount) {
        List<FileTreeNode> list = new ArrayList<>();
        findInsertDirectories(node, list);

        return expectedCount == list.size();
    }

    public boolean insertDirectoryCopyDirectory() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(true);

        return validateInsertDirectories(node, 1);
    }

    public boolean insertDirectoryRecreateAsDirectory() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY);
        when(node.isDirectory()).thenReturn(false);

        return validateInsertDirectories(node, 1);
    }

    public boolean insertDirectoryRemoveDirectory() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(true);

        return validateInsertDirectories(node, 0);
    }

    public boolean insertDirectoryCopyFile() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(false);

        return validateInsertDirectories(node, 0);
    }

    private boolean validateInsertFiles(DbCompareNode node, int expectedCount) {
        List<FileTreeNode> list = new ArrayList<>();
        findInsertFiles(node, list);

        return expectedCount == list.size();
    }

    public boolean insertFileRecreateAsFile() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_FILE);
        when(node.isDirectory()).thenReturn(false);

        return validateInsertFiles(node, 1);
    }

    public boolean insertFileCopyDirectory() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(true);

        return validateInsertFiles(node, 0);
    }

    public boolean insertFileRemoveFile() {
        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(false);

        return validateInsertFiles(node, 0);
    }
}
