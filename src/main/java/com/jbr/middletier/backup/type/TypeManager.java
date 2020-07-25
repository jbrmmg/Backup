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

    private final static String file_type = "file";
    private final static String database_type = "database";
    private final static String git_type = "git";
    private final static String clean_type = "clean";
    private final static String zipup_type = "zipup";

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
        if(type.equalsIgnoreCase(file_type)) {
            return fileBackup;
        }

        if(type.equalsIgnoreCase(database_type)) {
            return databaseBackup;
        }

        if(type.equalsIgnoreCase(git_type)) {
            return gitBackup;
        }

        if(type.equalsIgnoreCase(clean_type)) {
            return cleanBackup;
        }

        if(type.equalsIgnoreCase(zipup_type)) {
            return zipupBackup;
        }

        backupManager.postWebLog(BackupManager.webLogLevel.ERROR,String.format("%s invalid type requested.",type));
        throw new IllegalArgumentException(String.format("%s invalid type requested.",type));
    }
}
