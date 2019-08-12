package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import org.springframework.stereotype.Component;

/**
 * Created by jason on 11/02/17.
 */

@Component
public interface PerformBackup {
    void performBackup(BackupManager backupManager, Backup backup);
}
