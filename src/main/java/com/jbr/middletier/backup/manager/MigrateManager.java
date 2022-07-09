package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.dto.MigrateDateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MigrateManager {
    private static final Logger LOG = LoggerFactory.getLogger(MigrateManager.class);

    private final ActionManager actionManager;
    private final FileSystemObjectManager fileSystemObjectManager;
    private final FileSystem fileSystem;

    @Autowired
    public MigrateManager(FileSystemObjectManager fileSystemObjectManager,
                              ActionManager actionManager,
                              FileSystem fileSystem) {
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.actionManager = actionManager;
        this.fileSystem = fileSystem;
    }

    public List<MigrateDateDTO> postMigrationChecks() {
        List<MigrateDateDTO> result = new ArrayList<>();
        MigrateDateDTO migrateDateDTO = new MigrateDateDTO();
        result.add(migrateDateDTO);

        try {
            //TODO update the directories, remove '.' files and remove blank directories
        } catch(Exception e) {
            migrateDateDTO.setProblems();
        }

        return result;
    }
}
