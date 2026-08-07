package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.RunStatus;
import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.manager.FileSystem;

public interface PerformBackup {
    RunStatus performBackup(BackupManager backupManager, FileSystem fileSystem, Backup backup);

    String getSummary();

    String getType();
}
