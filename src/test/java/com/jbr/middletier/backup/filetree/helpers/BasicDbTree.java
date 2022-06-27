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

    public void test() {
        DbDirectory mockDbDirectory = mock(DbDirectory.class);
        when(mockDbDirectory.isDirectory()).thenReturn(true);

        FileTreeNode test = createCompareNode(CompareStatusType.REMOVED, null, mockDbDirectory, null);
        Assert.assertNotNull(test);

        try {
            createCompareNode(CompareStatusType.REMOVED, null, null, null);
            Assert.fail();
        } catch(IllegalStateException e) {
            Assert.assertEquals("Status is removed, but no source provided", e.getMessage());
        }

        test = createCompareNode(CompareStatusType.ADDED, null, null, mockDbDirectory);
        Assert.assertNotNull(test);

        try {
            createCompareNode(CompareStatusType.ADDED, null, null, null);
            Assert.fail();
        } catch(IllegalStateException e) {
            Assert.assertEquals("Status is added, but no destination provided", e.getMessage());
        }

        DbCompareNode node = mock(DbCompareNode.class);
        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(false);

        List<FileTreeNode> list = new ArrayList<>();

        findDeleteFiles(node, list);
        Assert.assertEquals(1, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY);
        when(node.isDirectory()).thenReturn(true);
        findDeleteFiles(node, list);
        Assert.assertEquals(1, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(true);
        findDeleteFiles(node, list);
        Assert.assertEquals(0, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(false);
        findDeleteFiles(node, list);
        Assert.assertEquals(0, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(true);
        findDeleteDirectories(node, list);
        Assert.assertEquals(1, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(true);
        findDeleteDirectories(node, list);
        Assert.assertEquals(0, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(false);
        findDeleteDirectories(node, list);
        Assert.assertEquals(0, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_FILE);
        when(node.isDirectory()).thenReturn(false);
        findDeleteDirectories(node, list);
        Assert.assertEquals(1, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(true);
        findInsertDirectories(node, list);
        Assert.assertEquals(1, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_DIRECTORY);
        when(node.isDirectory()).thenReturn(false);
        findInsertDirectories(node, list);
        Assert.assertEquals(1, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(true);
        findInsertDirectories(node, list);
        Assert.assertEquals(0, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(false);
        findInsertDirectories(node, list);
        Assert.assertEquals(0, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(false);
        findInsertFiles(node, list);
        Assert.assertEquals(1, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.RECREATE_AS_FILE);
        when(node.isDirectory()).thenReturn(false);
        findInsertFiles(node, list);
        Assert.assertEquals(1, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.COPY);
        when(node.isDirectory()).thenReturn(true);
        findInsertFiles(node, list);
        Assert.assertEquals(0, list.size());
        list.clear();

        when(node.getActionType()).thenReturn(DbCompareNode.ActionType.REMOVE);
        when(node.isDirectory()).thenReturn(false);
        findInsertFiles(node, list);
        Assert.assertEquals(0, list.size());
        list.clear();
    }
}
