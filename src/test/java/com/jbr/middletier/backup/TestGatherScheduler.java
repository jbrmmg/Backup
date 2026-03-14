package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.schedule.GatherSynchronizeCtrl;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestGatherScheduler extends WebTester {
    @Autowired
    GatherSynchronizeCtrl gatherSynchronizeCtrl;

    @Test
    void TestGather() {
        gatherSynchronizeCtrl.gatherCron();
        assertTrue(true);
    }
}
