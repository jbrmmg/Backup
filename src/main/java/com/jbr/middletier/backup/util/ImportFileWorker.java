package com.jbr.middletier.backup.util;

import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.ImportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportFileWorker implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFileWorker.class);

    private final ImportFileWorkQueue queue;
    private boolean running = false;

    public ImportFileWorker(ImportFileWorkQueue queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        this.running = true;

        // Process instructions from the queue.
        while (running) {
            if(queue.isEmpty()) {
                try {
                    queue.waitIsNotEmpty();
                } catch (InterruptedException e) {
//                    log.severe("Error while waiting to Consume messages.");
                    break;
                }
            }
            if (!running) {
                break;
            }
            PreImportFileDTO file = queue.poll();
            LOG.info("Have file {}", file.getFilename());

            // Process the message.
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            file.setId(20);
            file.update();
            LOG.info("Updated {}", file.getFilename());

            // Trigger an event to notify the client.
        }
    }

    public void stop() {
        this.running = false;
    }
}
