package com.jbr.middletier.backup;

import com.fasterxml.classmate.TypeResolver;
import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.config.SpringFoxConfig;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.Source;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;

import java.util.ArrayList;
import java.util.List;

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
    public void TestFSOFailure() {
        LOG.info("Test FSO Failure");
        try {
            FileSystemObjectType test = FileSystemObjectType.getFileSystemObjectType("BLAH");
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
