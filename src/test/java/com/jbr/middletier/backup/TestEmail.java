package com.jbr.middletier.backup;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("FieldCanBeLocal")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestEmail extends WebTester  {
    private static final Logger LOG = LoggerFactory.getLogger(TestEmail.class);

    @Autowired
    ActionConfirmRepository actionConfirmRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    DirectoryRepository directoryRepository;

    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    LocationRepository locationRepository;

    private GreenMail testSMTP;

    @Before
    public void setupSMTP() {
        testSMTP = new GreenMail(ServerSetupTest.SMTP);
        testSMTP.start();
    }

    @Test
    @Ignore
    public void TestEmail() {
        try {
            Optional<Location> location = locationRepository.findById(1);
            assertTrue(location.isPresent());

            LOG.info("Location {}", location.get());

            Source source = new Source();
            source.setId(1);
            source.setPath("/");
            source.setLocation(location.get());
            sourceRepository.save(source);

            LOG.info("Source {}", source);

            DirectoryInfo directory = new DirectoryInfo();
            directory.setSource(source);
            directory.setName("");
            directory.clearRemoved();
            directoryRepository.save(directory);

            LOG.info("Directory Info {}", directory);

            FileInfo file = new FileInfo();
            file.setName("Test");
            file.setDirectoryInfo(directory);
            file.clearRemoved();
            fileRepository.save(file);

            ActionConfirm action = new ActionConfirm();
            action.setAction("Test");
            action.setConfirmed(false);
            action.setFileInfo(file);
            actionConfirmRepository.save(action);

            getMockMvc().perform(post("/jbr/int/backup/actionemail")
                    .contentType(getContentType()))
                    .andExpect(status().isOk());

            actionConfirmRepository.deleteAll();
            fileRepository.deleteAll();
            directoryRepository.deleteAll();
            sourceRepository.deleteAll();
        } catch (Exception ex) {
            fail();
        }
    }
}
