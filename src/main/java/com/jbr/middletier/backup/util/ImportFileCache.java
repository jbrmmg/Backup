package com.jbr.middletier.backup.util;

import com.jbr.middletier.backup.dto.PreImportFileDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ImportFileCache {
    /*
     * Used to cache the details of the files in the import directory.
     */
    final private Map<String,ImportFileCacheEntry> cache;

    public ImportFileCache() {
        this.cache = new HashMap<>();
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
        this.cache.put(filename.toLowerCase(),new ImportFileCacheEntry(importFile));
    }

    public void remove(String filename) {
        this.cache.remove(filename.toLowerCase());
    }

    public Set<String> getFiles() {
        return this.cache.keySet();
    }
}
