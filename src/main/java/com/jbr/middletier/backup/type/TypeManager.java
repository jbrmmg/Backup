package com.jbr.middletier.backup.type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by jason on 11/02/17.
 */

@Component
public class TypeManager {
    private static final Logger LOG = LoggerFactory.getLogger(TypeManager.class);

    public static final String FILE_TYPE = "file";
    public static final String DATABASE_TYPE = "database";
    public static final String GIT_TYPE = "git";
    public static final String CLEAN_TYPE = "clean";
    public static final String ZIPUP_TYPE = "zipup";

    private final List<PerformBackup> performBackups;

    @Autowired
    public TypeManager(List<PerformBackup> performBackups) {
        this.performBackups = performBackups;
    }

    public PerformBackup getBackup(String type) {
        // Return the required type of backup.
        for(PerformBackup performBackup : performBackups){
            if(performBackup.getType().equalsIgnoreCase(type)){
                return performBackup;
            }
        }

        LOG.error("{} invalid type requested.", type);
        throw new IllegalArgumentException(String.format("%s invalid type requested.",type));
    }
}
