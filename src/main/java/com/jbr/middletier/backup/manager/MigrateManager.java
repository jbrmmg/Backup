package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.MigrateDateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

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
                LOG.info("Remove . file {}", nextFso);
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
                LOG.info("Remove blank directory {}", nextFso);
            }
        }
    }

    static class DirectoryLayerInfo {
        private final String newName;
        private final String parentId;
        private final List<String> newLayers;

        public DirectoryLayerInfo(String oldName, String parentId) {
            this.newLayers = new ArrayList<>();

            String[] layers = oldName.split("/");

            for(int i = 0; i < layers.length - 1; i++) {
                if(layers[i].length() > 0) {
                    newLayers.add(layers[i]);
                }
            }

            this.newName = layers[layers.length - 1];
            this.parentId = parentId;
        }

        public String getNewName() {
            return newName;
        }

        public int getLayerCount() {
            return newLayers.size();
        }

        public String getNameAt(int index) {
            if(index == newLayers.size()) {
                return this.newName;
            }

            return newLayers.get(index);
        }

        public String getPathUpTo(int index) {
            StringBuilder result = new StringBuilder();

            result.append(this.parentId);
            result.append(":");
            for(int i = 0; i < index; i++) {
                if(i > 0) {
                    result.append("/");
                }
                result.append(getNameAt(i));
            }

            return result.toString();
        }
    }

    private DirectoryInfo addRequired(DirectoryInfo newDirectory, String pathToNew, Map<String,DirectoryInfo> alreadyAdded) {
        if(alreadyAdded.containsKey(pathToNew)) {
            return alreadyAdded.get(pathToNew);
        }

        alreadyAdded.put(pathToNew, newDirectory);
        return null;
    }

    public void updateDirectories(MigrateDateDTO migrateDateDTO) {
        Map<String,DirectoryInfo> addedDirectories = new HashMap<>();

        // Update directories where there are multiple
        for(FileSystemObject nextFso : fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_DIRECTORY)) {
            if(nextFso.getName().contains("/")) {
                LOG.info("Process {}", nextFso.getName());
                DirectoryLayerInfo directoryLayerInfo = new DirectoryLayerInfo(nextFso.getName(),nextFso.getParentId().toString());

                FileSystemObjectId previousParentId = nextFso.getParentId();
                for(int i = 0; i < directoryLayerInfo.getLayerCount(); i++) {
                    DirectoryInfo newDirectory = new DirectoryInfo();
                    newDirectory.setName(directoryLayerInfo.getNameAt(i));
                    newDirectory.setParentId(previousParentId);
                    newDirectory.clearRemoved();

                    DirectoryInfo existingDirectory = addRequired(newDirectory, directoryLayerInfo.getPathUpTo(i + 1), addedDirectories);
                    if(existingDirectory == null) {
                        fileSystemObjectManager.save(newDirectory);
                        previousParentId = newDirectory.getIdAndType();
                        migrateDateDTO.increment(MigrateDateDTO.MigrateDataCountType.NEW_DIRECTORIES);
                    } else {
                        previousParentId = existingDirectory.getIdAndType();
                    }
                }

                DirectoryInfo nextFsoDirectory = (DirectoryInfo)nextFso;
                nextFsoDirectory.setParentId(previousParentId);
                nextFsoDirectory.setName(directoryLayerInfo.getNewName());
                fileSystemObjectManager.save(nextFso);
                migrateDateDTO.increment(MigrateDateDTO.MigrateDataCountType.DIRECTORIES_UPDATED);
                LOG.info("Restructured {}", nextFso);
            }
        }
    }

    public List<MigrateDateDTO> postMigrationChecks() {
        List<MigrateDateDTO> result = new ArrayList<>();
        MigrateDateDTO migrateDateDTO = new MigrateDateDTO();
        result.add(migrateDateDTO);

        try {
            LOG.info("Remove the . files");
            removeDotFiles(migrateDateDTO);
            LOG.info("Remove the blank directories");
            removeBlankDirectories(migrateDateDTO);
            LOG.info("Update directories");
            updateDirectories(migrateDateDTO);
        } catch(Exception e) {
            migrateDateDTO.setProblems();
        }

        return result;
    }
}
