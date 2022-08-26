package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.DbLoggingManager;
import com.jbr.middletier.backup.manager.FileSystem;
import org.springframework.stereotype.Component;

/**
 * Created by jason on 11/02/17.
 */

@Component
public interface PerformBackup {
    void performBackup(BackupManager backupManager, DbLoggingManager loggingManager, FileSystem fileSystem, Backup backup);
}
