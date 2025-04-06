package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.dto.PreImportFileDTO;

import java.time.LocalDateTime;

public class ImportFileCacheEntry {
    private final PreImportFileDTO importFile;
    private final LocalDateTime timestamp;

    public ImportFileCacheEntry(PreImportFileDTO importFile) {
        this.importFile = importFile;
        this.timestamp = LocalDateTime.now();
    }

    public PreImportFileDTO getImportFile() {
        return this.importFile;
    }

    public boolean expired() {
        return this.timestamp.isBefore(LocalDateTime.now().minusHours(1));
    }
}
