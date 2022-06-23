package com.jbr.middletier.backup;

import com.jbr.middletier.backup.config.ApplicationProperties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestProperties {
    @Test
    public void TestPropertyClass() {
        ApplicationProperties test = new ApplicationProperties();
        test.setDbBackupCommand("DBCmd");
        assertEquals("DBCmd",test.getDbBackupCommand());
        test.setDbBackupMaxTime(10L);
        assertEquals((Long)10L,test.getDbBackupMaxTime());
        test.setDbPassword("passwd");
        assertEquals("passwd",test.getDbPassword());
        test.setDbUrl("url");
        assertEquals("url",test.getDbUrl());
        test.setDbUsername("usr");
        assertEquals("usr",test.getDbUsername());
        test.setEnabled(true);
        assertTrue(test.getEnabled());
        test.setGatherEnabled(true);
        assertTrue(test.getGatherEnabled());
        test.setGatherSchedule("schedule");
        assertEquals("schedule",test.getGatherSchedule());
        test.setSchedule("schedule");
        assertEquals("schedule",test.getSchedule());
        test.setReviewDirectory("directory");
        assertEquals("directory",test.getReviewDirectory());
        test.setServiceName("service");
        assertEquals("service",test.getServiceName());
        test.setWebLogUrl("url");
        assertEquals("url",test.getWebLogUrl());
        test.getDirectory().setDateFormat("dd/mm");
        assertEquals("dd/mm",test.getDirectory().getDateFormat());
        test.getDirectory().setDays(1);
        assertEquals(1,test.getDirectory().getDays());
        test.getDirectory().setName("synchronise");
        assertEquals("synchronise",test.getDirectory().getName());
        test.getDirectory().setZip("zip");
        assertEquals("zip",test.getDirectory().getZip());
        test.getEmail().setAuthenticate(true);
        assertTrue(test.getEmail().getAuthenticate());
        test.getEmail().setEnabled(true);
        assertTrue(test.getEmail().getEnabled());
        test.getEmail().setFrom("from");
        assertEquals("from", test.getEmail().getFrom());
        test.getEmail().setHost("host");
        assertEquals("host",test.getEmail().getHost());
        test.getEmail().setPassword("passwd");
        assertEquals("passwd",test.getEmail().getPassword());
        test.getEmail().setPort(900);
        assertEquals((Integer) 900,test.getEmail().getPort());
        test.getEmail().setTo("to");
        assertEquals("to",test.getEmail().getTo());
        test.getEmail().setUser("usr");
        assertEquals("usr",test.getEmail().getUser());
    }
}
