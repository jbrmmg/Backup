package com.jbr.middletier.backup.data;

import com.jbr.middletier.backup.control.DriveController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class SynchronizeStatus {
    final static private Logger LOG = LoggerFactory.getLogger(SynchronizeStatus.class);

    public String name;
    public String path;
    public String action;
    public Long size1;
    public Date date1;
    public Long size2;
    public Date date2;
    public String md51;
    public String md52;

    public SynchronizeStatus(String name,
                             String path,
                             String action,
                             Long size1,
                             Date date1,
                             Long size2,
                             Date date2,
                             String md51,
                             String md52) {
        this.name = name;
        this.path = path;
        this.action = action;
        this.size1 = size1;
        this.date1 = date1;
        this.size2 = size2;
        this.date2 = date2;
        this.md51 = md51;
        this.md52 = md52;
    }
}
