package com.jbr.middletier.backup.util;

import com.jbr.middletier.backup.dto.PreImportFileDTO;

import java.util.LinkedList;
import java.util.Queue;

public class ImportFileWorkQueue {
    private final Queue<PreImportFileDTO> queue = new LinkedList<>();
    private final Queue<PreImportFileDTO> backupQueue = new LinkedList<>();
    private boolean useBackup;
    private final Object IS_NOT_EMPTY = new Object();

    public ImportFileWorkQueue() {
        useBackup = true;
    }

    // other methods
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
