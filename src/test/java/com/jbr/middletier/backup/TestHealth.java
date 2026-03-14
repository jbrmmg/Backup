package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.health.ServiceHealthIndicator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MiddleTier.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestHealth extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(TestHealth.class);

    @Test
    void TestHealthURL() throws Exception {
        getMockMvc().perform(get("/actuator/health")
                .contentType(getContentType()))
                .andExpect(status().isOk());
    }

    @Test
    void TestHealthObject() {
        BackupRepository backupRepository = mock(BackupRepository.class);
        when(backupRepository.findAll()).thenThrow(IllegalStateException.class);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
        when(applicationProperties.getServiceName()).thenReturn("Testing");

        ServiceHealthIndicator serviceHealthIndicator = new ServiceHealthIndicator(backupRepository, applicationProperties);

        Health result = serviceHealthIndicator.health();
        assertEquals(Status.DOWN,result.getStatus());
    }

    @Test
    void TestFSOFailure() {
        LOG.info("Test FSO Failure");
        try {
            FileSystemObjectType.getFileSystemObjectType("BLAH");
            fail();
        } catch (IllegalStateException e) {
            LOG.info("This is expected");
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    void TestSourceCTOR() {
        LOG.info("Test FSO Failure");
        try {
            Source source = new Source();
            source.setPath("TestWithPath");
            assertEquals("TestWithPath", source.getPath());
        } catch (Exception e) {
            fail();
        }
    }
}
