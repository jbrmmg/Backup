package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ImportFileCache {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFileCache.class);

    /*
     * Used to cache the details of the files in the import directory.
     */
    private final Map<String,ImportFileCacheEntry> cache;
    private final ImportFileWorkQueue importFileWorkQueue;
    private final ApplicationProperties applicationProperties;

    public ImportFileCache(ImportFileWorkQueue importFileWorkQueue,
                           ApplicationProperties applicationProperties) {
        this.cache = new HashMap<>();
        this.importFileWorkQueue = importFileWorkQueue;
        this.applicationProperties = applicationProperties;
    }

    public boolean containsKey(String filename) {
        if(this.cache.containsKey(filename.toLowerCase())){
            ImportFileCacheEntry entry = this.cache.get(filename.toLowerCase());

            if(entry.expired()) {
                this.cache.remove(filename.toLowerCase());
                return false;
            }

            return true;
        }

        return false;
    }

    public PreImportFileDTO get(String filename) {
        if(this.cache.containsKey(filename.toLowerCase())){
            return this.cache.get(filename.toLowerCase()).getImportFile();
        }

        throw new IllegalArgumentException("Requested an invalid filename from cache.");
    }

    public void put(String filename, PreImportFileDTO importFile) {
        this.cache.put(filename.toLowerCase(), new ImportFileCacheEntry(importFile));
        this.queueForUpdates(importFile);
    }

    public void remove(String filename) {
        this.cache.remove(filename.toLowerCase());
    }

    public Set<String> getFiles() {
        return this.cache.keySet();
    }

    public void queueForUpdates(PreImportFileDTO importFile) {
        try {
            this.importFileWorkQueue.put(importFile);
        } catch (InterruptedException e) {
            LOG.info("Interrupted queue");
            Thread.currentThread().interrupt();
        }
    }

    public void clear() {
        this.cache.clear();
    }

    public int inQueue() {
        return this.importFileWorkQueue.itemsInQueue();
    }

    public boolean loadCacheOnStartup() {
        // If not specified, default to true.
        if(this.applicationProperties.getLoadCacheOnStartup() == null) {
            return true;
        }

        return this.applicationProperties.getLoadCacheOnStartup();
    }
}
