package com.jbr.middletier.backup.summary;

import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public class Summary {
    private static final Logger LOG = LoggerFactory.getLogger(Summary.class);

    boolean valid;
    Date validAt;
    List<SourceDTO> sources;

    private static class SummaryInitialiser implements Runnable {
        private final AssociatedFileDataManager associatedFileDataManager;
        private final Summary instance;

        public SummaryInitialiser(Summary instance, AssociatedFileDataManager associatedFileDataManager) {
            this.associatedFileDataManager = associatedFileDataManager;
            this.instance = instance;
        }

        @Override
        public void run() {
            try {
                instance.sources = new ArrayList<>();

                // Initialise the summary object.
                for (Source nextSource : associatedFileDataManager.internalFindAllSource()) {
                    SourceDTO nextSourceDTO = nextSource.getSourceDTO();
                    instance.sources.add(nextSourceDTO);

                    nextSourceDTO.incrementDirectoryCount();
                    nextSourceDTO.incrementFileCount();
                }

                // Set the object to valid.
                instance.validAt = new Date();
                instance.valid = true;
            } catch(Exception e) {
                LOG.error("Failed to get the summary: ",e);
            }
        }
    }

    private static Summary instance = null;

    public static Summary getInstance(AssociatedFileDataManager associatedFileDataManager) {
        if(instance != null) {
            return instance;
        }

        instance = new Summary();

        SummaryInitialiser initialiser = new SummaryInitialiser(instance, associatedFileDataManager);
        new Thread(initialiser).start();

        return instance;
    }

    private Summary() {
        LOG.info("Initialise the summary data.");
        this.valid = false;
    }

    public Date getValidAt() {
        return validAt;
    }

    public boolean isValid() {
        return this.valid;
    }

    public List<SourceDTO> getSources() {
        return Collections.unmodifiableList(this.sources);
    }
}
