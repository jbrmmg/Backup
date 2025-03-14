package com.jbr.middletier.backup.manager.importing;

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

    @Autowired
    public ImportFileWorkerManager(ImportManager manager, ImportFileWorkQueue queue, FileSystem fileSystem) {
        this.manager = manager;
        this.queue = queue;
        this.fileSystem = fileSystem;
        this.workers = new ArrayList<>();
    }

    @PostConstruct
    private void init() {
        // Create the threads.
        for(int i = 0; i < 10; i++) {
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
