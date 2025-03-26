package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.importing.step.ImportStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ImportFileWorkQueue {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFileWorkQueue.class);

    private final BlockingQueue<PreImportFileDTO> queue = new LinkedBlockingQueue<>();
    private final List<ImportStep> stepProcessors;

    @Autowired
    public ImportFileWorkQueue(List<ImportStep> stepProcessors) {
        this.stepProcessors = stepProcessors;
    }

    public void put(PreImportFileDTO file) throws InterruptedException {
        queue.put(file);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public PreImportFileDTO take() throws InterruptedException {
        return queue.take();
    }

    public ImportStep getStepProcessor(FileProcessingStepType stepType) {
        for (ImportStep step : stepProcessors) {
            if(step.getStepType().equals(stepType)) {
                return step;
            }
        }

        return null;
    }

    public int itemsInQueue() {
        return this.queue.size();
    }
}
