package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFileTree {

    class TreeNode {
        TreeNode parent;
        List<TreeNode> children;

        protected TreeNode() {
            parent = null;
            children = new ArrayList<>();
        }

        public void addChild(TreeNode child) {
            children.add(child);
        }
    }

    class TreeNodeRoot extends  TreeNode {
        public TreeNodeRoot() {
           super();
        }
    }


    @Test
    public void TestGather() {
    }
}
