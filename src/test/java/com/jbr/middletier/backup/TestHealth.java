package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.health.ServiceHealthIndicator;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestHealth extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(TestHealth.class);

    @Test
    public void TestHealthURL() throws Exception {
        getMockMvc().perform(get("/actuator/health")
                .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    @Test
    public void TestHealthObject() {
        BackupRepository backupRepository = mock(BackupRepository.class);
        when(backupRepository.findAll()).thenThrow(IllegalStateException.class);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        when(applicationProperties.getServiceName()).thenReturn("Testing");

        ServiceHealthIndicator serviceHealthIndicator = new ServiceHealthIndicator(backupRepository, applicationProperties);

        Health result = serviceHealthIndicator.health();
        Assert.assertEquals(Status.DOWN,result.getStatus());
    }

    @Test
    public void TestFSOFailure() {
        LOG.info("Test FSO Failure");
        try {
            FileSystemObjectType.getFileSystemObjectType("BLAH");
            Assert.fail();
        } catch (IllegalStateException e) {
            LOG.info("This is expected");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void TestSourceCTOR() {
        LOG.info("Test FSO Failure");
        try {
            Source source = new Source("TestWithPath");
            Assert.assertEquals("TestWithPath", source.getPath());
        } catch (Exception e) {
            Assert.fail();
        }
    }
}
