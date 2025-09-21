package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.importing.step.ImportStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportFileWorker implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFileWorker.class);

    private final ImportFileWorkQueue queue;
    private final ImportFileCache cache;

    public ImportFileWorker(ImportFileWorkQueue queue,
                            ImportFileCache cache) {
        this.queue = queue;
        this.cache = cache;
    }

    private void performStep (FileProcessingStepType nextUnknown, PreImportFileDTO file) {
        try {
            ImportStep nextStep = this.queue.getStepProcessor(nextUnknown);

            if (nextStep != null) {
                LOG.info("Performing step {} for {}", nextStep.getStepType(), file.getFilename());
                file.setStepStatus(nextUnknown, nextStep.performStep(file));
                LOG.debug("Completed step {} for {}", nextStep.getStepType(), file.getFilename());

                if(nextStep.getStepType() != FileProcessingStepType.FPS_FINAL_UPDATE) {
                    this.cache.queueForUpdates(file);
                }
            } else {
                throw new IllegalStateException("There is no processor for step " + nextUnknown);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
            file.setStepStatus(nextUnknown, TrafficLightType.TL_RED);
        }
    }

    private void processFile(PreImportFileDTO file) {
        try {
            if(file == null) {
                return;
            }

            // Get the next unknown step.
            FileProcessingStepType nextUnknown = file.getNextUnknownStep();
            performStep(nextUnknown, file);
        } catch (Exception e) {
            LOG.warn(e.getMessage(),e);
        }
    }

    @Override
    public void run() {
        try {
            // Process instructions from the queue.
            while (true) {
                PreImportFileDTO nextItem = queue.take();
                if (nextItem == null) {
                    LOG.info("Next is null");
                } else {
                    LOG.info("Processing next item {}", nextItem.getFilename());
                }

                if(nextItem != null && nextItem.isStopMarker()) {
                    break;
                }
                processFile(nextItem);
            }
        } catch(InterruptedException e) {
            LOG.info("Interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
