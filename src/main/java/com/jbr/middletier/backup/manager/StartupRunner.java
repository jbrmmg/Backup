package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.data.SourceStatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(StartupRunner.class);

    private final AssociatedFileDataManager associatedFileDataManager;

    @Autowired
    StartupRunner(AssociatedFileDataManager associatedFileDataManager) {
        this.associatedFileDataManager = associatedFileDataManager;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check the status of all the sources, if any are still set to GATHERING then they
        // should be set to OK.
        for(Source nextSource: associatedFileDataManager.findAllSource()) {
            if(nextSource.getIdAndType().getType() == FileSystemObjectType.FSO_SOURCE) {
                LOG.info("Check Source {}", nextSource);
                if(nextSource.getStatus().equals(SourceStatusType.SST_GATHERING)) {
                    nextSource.setStatus(SourceStatusType.SST_OK);
                    associatedFileDataManager.updateSource(nextSource);
                }
            }
        }
    }
}
