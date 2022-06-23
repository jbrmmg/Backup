package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.manager.BackupManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class TypeManager {

    private static final String FILE_TYPE = "file";
    private static final String DATABASE_TYPE = "database";
    private static final String GIT_TYPE = "git";
    private static final String CLEAN_TYPE = "clean";
    private static final String ZIPUP_TYPE = "zipup";

    private final PerformBackup fileBackup;

    private final PerformBackup databaseBackup;

    private final PerformBackup gitBackup;

    private final PerformBackup cleanBackup;

    private final PerformBackup zipupBackup;

    private final BackupManager backupManager;

    @Autowired
    public TypeManager(PerformBackup fileBackup,
                       PerformBackup databaseBackup,
                       PerformBackup gitBackup,
                       PerformBackup cleanBackup,
                       PerformBackup zipupBackup,
                       BackupManager backupManager) {
        this.fileBackup = fileBackup;
        this.databaseBackup = databaseBackup;
        this.gitBackup = gitBackup;
        this.cleanBackup = cleanBackup;
        this.zipupBackup = zipupBackup;
        this.backupManager = backupManager;
    }

    public PerformBackup getBackup(String type) {
        // Return the required type of backup.
        if(type.equalsIgnoreCase(FILE_TYPE)) {
            return fileBackup;
        }

        if(type.equalsIgnoreCase(DATABASE_TYPE)) {
            return databaseBackup;
        }

        if(type.equalsIgnoreCase(GIT_TYPE)) {
            return gitBackup;
        }

        if(type.equalsIgnoreCase(CLEAN_TYPE)) {
            return cleanBackup;
        }

        if(type.equalsIgnoreCase(ZIPUP_TYPE)) {
            return zipupBackup;
        }

        backupManager.postWebLog(BackupManager.webLogLevel.ERROR,String.format("%s invalid type requested.",type));
        throw new IllegalArgumentException(String.format("%s invalid type requested.",type));
    }
}
