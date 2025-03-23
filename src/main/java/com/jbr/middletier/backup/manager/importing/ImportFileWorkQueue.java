package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.importing.step.ImportStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Component
public class ImportFileWorkQueue {
    private final Queue<PreImportFileDTO> queue = new LinkedList<>();
    private final Object IS_NOT_EMPTY = new Object();

    private final List<ImportStep> stepProcessors;

    @Autowired
    public ImportFileWorkQueue(List<ImportStep> stepProcessors) {
        this.stepProcessors = stepProcessors;
    }

    public void add(PreImportFileDTO file) {
        queue.add(file);
        notifyIsNotEmpty();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void waitIsNotEmpty() throws InterruptedException {
        synchronized (IS_NOT_EMPTY) {
            IS_NOT_EMPTY.wait();
        }
    }

    public void notifyIsNotEmpty() {
        synchronized (IS_NOT_EMPTY) {
            IS_NOT_EMPTY.notify();
        }
    }

    public PreImportFileDTO poll() {
        return queue.poll();
    }

    public void clear() {
        queue.clear();
    }

    public ImportStep getStepProcessor(FileProcessingStepType stepType) {
        for (ImportStep step : stepProcessors) {
            if(step.getStepType().equals(stepType)) {
                return step;
            }
        }

        return null;
    }
}
