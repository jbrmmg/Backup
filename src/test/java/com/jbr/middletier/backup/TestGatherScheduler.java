package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.schedule.GatherSynchronizeCtrl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static junit.framework.TestCase.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestGatherScheduler extends WebTester {
    @Autowired
    GatherSynchronizeCtrl gatherSynchronizeCtrl;

    @Test
    public void testGather() {
        gatherSynchronizeCtrl.gatherCron();
        assertTrue(true);
    }
}
