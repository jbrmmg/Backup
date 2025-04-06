package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Component
public class ImportFileWorkerManager {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFileWorkerManager.class);

    private final List<ImportFileWorker> workers;
    private final ImportFileWorkQueue queue;
    private final ApplicationProperties applicationProperties;
    private final ImportFileCache cache;

    @Autowired
    public ImportFileWorkerManager(ImportFileWorkQueue queue,
                                   ApplicationProperties applicationProperties,
                                   ImportFileCache importFileCache) {
        this.queue = queue;
        this.applicationProperties = applicationProperties;
        this.cache = importFileCache;
        this.workers = new ArrayList<>();
    }

    @PostConstruct
    private void init() {
        // Set up the threads if the count is greater than zero.
        if(applicationProperties.getImportThreads() != null &&  applicationProperties.getImportThreads() > 0) {
            // Create the threads.
            for(int i = 0; i < applicationProperties.getImportThreads(); i++) {
                ImportFileWorker worker = new ImportFileWorker(queue,cache);
                workers.add(worker);
            }

            // Startup the threads.
            workers.forEach(t -> {
                Thread thread = new Thread(t);
                thread.start();
            });
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            // Send a shutdown to the queue for each thread.
            if (applicationProperties.getImportThreads() != null && applicationProperties.getImportThreads() > 0) {
                // Create the threads.
                for (int i = 0; i < applicationProperties.getImportThreads(); i++) {
                    queue.put(new PreImportFileDTO(true));
                }
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("Error while waiting for worker threads to finish.", e);
        }
    }
}
