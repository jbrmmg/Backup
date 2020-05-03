package com.jbr.middletier.backup.type;

import com.jbr.middletier.backup.manager.BackupManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by jason on 11/02/17.
 */

@SuppressWarnings("FieldCanBeLocal")
@Component
public class TypeManager {

    private final String FILE_TYPE = "file";
    private final String DATABASE_TYPE = "database";
    private final String GIT_TYPE = "git";
    private final String CLEAN_TYPE = "clean";
    private final String NAS_TYPE = "nas";
    private final String ZIPUP_TYPE = "zipup";

    private final PerformBackup fileBackup;

    private final PerformBackup databaseBackup;

    private final PerformBackup gitBackup;

    private final PerformBackup cleanBackup;

    private final PerformBackup nasBackup;

    private final PerformBackup zipupBackup;

    private final BackupManager backupManager;

    @Autowired
    public TypeManager(PerformBackup fileBackup,
                       PerformBackup databaseBackup,
                       PerformBackup gitBackup,
                       PerformBackup cleanBackup,
                       PerformBackup nasBackup,
                       PerformBackup zipupBackup,
                       BackupManager backupManager) {
        this.fileBackup = fileBackup;
        this.databaseBackup = databaseBackup;
        this.gitBackup = gitBackup;
        this.cleanBackup = cleanBackup;
        this.nasBackup = nasBackup;
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

        if(type.equalsIgnoreCase(NAS_TYPE)) {
            return nasBackup;
        }

        if(type.equalsIgnoreCase(ZIPUP_TYPE)) {
            return zipupBackup;
        }

        backupManager.postWebLog(BackupManager.webLogLevel.ERROR,String.format("%s invalid type requested.",type));
        throw new IllegalArgumentException(String.format("%s invalid type requested.",type));
    }
}
