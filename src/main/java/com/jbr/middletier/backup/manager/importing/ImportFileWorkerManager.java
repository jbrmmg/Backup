package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.manager.FileSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class ImportFileWorkerManager {
    private final ImportManager manager;
    private final ImportFileWorkQueue queue;
    private final FileSystem fileSystem;
    private final List<ImportFileWorker> workers;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public ImportFileWorkerManager(ImportManager manager,
                                   ImportFileWorkQueue queue,
                                   FileSystem fileSystem,
                                   ApplicationProperties applicationProperties) {
        this.manager = manager;
        this.queue = queue;
        this.fileSystem = fileSystem;
        this.applicationProperties = applicationProperties;
        this.workers = new ArrayList<>();
    }

    @PostConstruct
    private void init() {
        // Set up the threads if the count is greater than zero.
        if(applicationProperties.getImportThreads() != null &&  applicationProperties.getImportThreads() > 0) {
            // Create the threads.
            for(int i = 0; i < applicationProperties.getImportThreads(); i++) {
                ImportFileWorker worker = new ImportFileWorker(queue,manager,fileSystem);
                workers.add(worker);
            }

            // Startup the threads.
            workers.forEach((t) -> {
                Thread thread = new Thread(t);
                thread.start();
            });
        }
    }
}
