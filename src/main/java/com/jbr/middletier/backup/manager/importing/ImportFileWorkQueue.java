package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.dto.PreImportFileDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Component
public class ImportFileWorkQueue {
    private final Queue<PreImportFileDTO> queue = new LinkedList<>();
    private final Queue<PreImportFileDTO> backupQueue = new LinkedList<>();
    private boolean useBackup;
    private final Object IS_NOT_EMPTY = new Object();
    private List<ImportFileWorker> workers;

    private void createThreads() {
        if(this.workers.size() < 10) {

        }
    }

    public ImportFileWorkQueue() {
        useBackup = true;
        workers = new LinkedList<>();
    }

    public void add(PreImportFileDTO file) {
        if(useBackup){
            backupQueue.add(file);
        } else {
            queue.add(file);
            notifyIsNotEmpty();
        }
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

    public void restart() {
        this.useBackup = false;

        // Transfer from backup queue
        boolean transfer = true;
        while(transfer) {
            PreImportFileDTO next = this.backupQueue.poll();

            if(next == null){
                transfer = false;
            } else {
                add(next);
            }
        }
    }
}
