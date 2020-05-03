package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.manager.BackupManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class NasBackup implements PerformBackup {

    @Override
    public void performBackup(BackupManager backupManager, Backup backup) {

    }
}
