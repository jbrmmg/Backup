package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestHealth extends WebTester {
    @Test
    @Ignore
    public void TestHealthURL() throws Exception {
        getMockMvc().perform(get("/actuator/health")
                .contentType(getContentType()))
                .andExpect(status().isOk());
    }
}
