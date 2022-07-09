package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
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

    private final FileSystemObjectManager fileSystemObjectManager;

    @Autowired
    public MigrateManager(FileSystemObjectManager fileSystemObjectManager) {
        this.fileSystemObjectManager = fileSystemObjectManager;
    }

    public void removeDotFiles(MigrateDateDTO migrateDateDTO) {
        // Remove '.' files.
        for(FileSystemObject nextFso : fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_FILE)) {
            if(nextFso.getName().equals(".")) {
                migrateDateDTO.increment(MigrateDateDTO.MigrateDataCountType.DOT_FILES_REMOVED);
                fileSystemObjectManager.delete(nextFso);
            }
        }
    }

    public void removeBlankDirectories(MigrateDateDTO migrateDateDTO) {
        // Remove blank directories
        for(FileSystemObject nextFso : fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_DIRECTORY)) {
            if(nextFso.getName().equals("")) {
                migrateDateDTO.increment(MigrateDateDTO.MigrateDataCountType.BLANKS_REMOVED);

                List<DirectoryInfo> directories = new ArrayList<>();
                List<FileInfo> files = new ArrayList<>();
                fileSystemObjectManager.loadImmediateByParent(nextFso.getIdAndType().getId(), directories, files);

                for(DirectoryInfo nextDirectory : directories) {
                    nextDirectory.setParentId(nextFso.getParentId());
                }
                for(FileInfo nextFile : files) {
                    nextFile.setParentId(nextFso.getParentId());
                }
                fileSystemObjectManager.delete(nextFso);
            }
        }
    }

    static class DirectoryLayerInfo {
        public String newName;
        public List<String> newLayers;

        public DirectoryLayerInfo(String oldName) {
            newLayers = new ArrayList<>();

            String[] layers = oldName.split("/");

            for(int i = 0; i < layers.length - 1; i++) {
                if(layers[i].length() > 0) {
                    newLayers.add(layers[i]);
                }
            }

            newName = layers[layers.length - 1];
        }
    }

    public void updateDirectories(MigrateDateDTO migrateDateDTO) {
        // Update directories where there are multiple
        for(FileSystemObject nextFso : fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_DIRECTORY)) {
            if(nextFso.getName().contains("/")) {
                DirectoryLayerInfo directoryLayerInfo = new DirectoryLayerInfo(nextFso.getName());

                FileSystemObjectId previousParentId = nextFso.getParentId();
                for(String nextLayerName : directoryLayerInfo.newLayers) {
                    DirectoryInfo newDirectory = new DirectoryInfo();
                    newDirectory.setName(nextLayerName);
                    newDirectory.setParentId(previousParentId);
                    newDirectory.clearRemoved();

                    fileSystemObjectManager.save(newDirectory);
                    previousParentId = newDirectory.getIdAndType();
                    migrateDateDTO.increment(MigrateDateDTO.MigrateDataCountType.NEW_DIRECTORIES);
                }

                DirectoryInfo nextFsoDirectory = (DirectoryInfo)nextFso;
                nextFsoDirectory.setParentId(previousParentId);
                nextFsoDirectory.setName(directoryLayerInfo.newName);
                fileSystemObjectManager.save(nextFso);
                migrateDateDTO.increment(MigrateDateDTO.MigrateDataCountType.DIRECTORIES_UPDATED);
            }
        }
    }

    public List<MigrateDateDTO> postMigrationChecks() {
        List<MigrateDateDTO> result = new ArrayList<>();
        MigrateDateDTO migrateDateDTO = new MigrateDateDTO();
        result.add(migrateDateDTO);

        try {
            removeDotFiles(migrateDateDTO);
            removeBlankDirectories(migrateDateDTO);
            updateDirectories(migrateDateDTO);
        } catch(Exception e) {
            migrateDateDTO.setProblems();
        }

        return result;
    }
}
