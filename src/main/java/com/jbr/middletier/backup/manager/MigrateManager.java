package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.MigrateDateDTO;
import com.jbr.middletier.backup.filetree.FileTreeNode;
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

    static class DirectoryNode extends FileTreeNode {
        private final String directoryName;
        private final FileSystemObjectId sourceId;
        private DirectoryInfo dbDirectory;

        static class DirectoryLevelHelper {
            private final List<String> levels;
            private int currentLevel;

            public DirectoryLevelHelper (DirectoryInfo directory) {
                this.levels = new ArrayList<>();
                this.currentLevel = 0;

                for(String nextLevel : directory.getName().split("/")) {
                    if(nextLevel.trim().length() > 0) {
                        levels.add(nextLevel);
                    }
                }
            }

            public String getName() {
                return levels.get(currentLevel);
            }

            public void nextLevel() {
                this.currentLevel++;
            }

            public boolean lastLevel() {
                return this.currentLevel == this.levels.size() - 1;
            }
        }

        protected DirectoryNode(DirectoryNode parent, FileSystemObjectId sourceId) {
            super(parent);
            this.directoryName = sourceId.toString();
            this.sourceId = sourceId;
        }

        private DirectoryNode(DirectoryNode parent, DirectoryLevelHelper directoryLevelHelper, DirectoryInfo directory) {
            super(parent);

            this.directoryName = directoryLevelHelper.getName();
            this.sourceId = parent.getSourceId();

            if(directoryLevelHelper.lastLevel()) {
                this.dbDirectory = directory;
            } else {
                directoryLevelHelper.nextLevel();
                addDirectory(directoryLevelHelper, directory);
            }
        }

        @Override
        public String getName() {
            return this.directoryName;
        }

        @Override
        protected void childAdded(FileTreeNode newChild) {
            // No implementation required
        }

        private void addDirectory(DirectoryLevelHelper directoryLevelHelper, DirectoryInfo directory) {
            DirectoryNode nextNode = (DirectoryNode) getNamedChild(directoryLevelHelper.getName());
            if(nextNode == null) {
                addChild(new DirectoryNode(this, directoryLevelHelper, directory));
            } else {
                directoryLevelHelper.nextLevel();
                nextNode.addDirectory(directoryLevelHelper, directory);
            }
        }

        public void addDirectory(DirectoryInfo directory) {
            addDirectory(new DirectoryLevelHelper(directory), directory);
        }

        public DirectoryInfo getDbDirectory() {
            return this.dbDirectory;
        }

        public FileSystemObjectId getSourceId() {
            return this.sourceId;
        }

        public DirectoryInfo getDbDirectory(FileSystemObjectId parentId) {
            this.dbDirectory.setName(this.directoryName);
            this.dbDirectory.setParentId(parentId);

            return this.dbDirectory;
        }

        public FileSystemObjectId getIdForParent() {
            if(this.dbDirectory != null) {
                return this.dbDirectory.getIdAndType();
            }

            return this.sourceId;
        }

        public void createDbDirectory() {
            this.dbDirectory = new DirectoryInfo();
            this.dbDirectory.clearRemoved();
        }
    }

    public void processTree(DirectoryNode root, MigrateDateDTO migrateDateDTO) {
        // Process the children.
        for(FileTreeNode nextChild : root.getChildren()) {
            DirectoryNode nextDirectoryChild = (DirectoryNode) nextChild;

            // Does this have a database value?
            if(nextDirectoryChild.getDbDirectory() == null) {
                nextDirectoryChild.createDbDirectory();
                migrateDateDTO.increment(MigrateDateDTO.MigrateDataCountType.NEW_DIRECTORIES);
                LOG.info("Create new directory {}", nextDirectoryChild.getName());
            } else {
                migrateDateDTO.increment(MigrateDateDTO.MigrateDataCountType.DIRECTORIES_UPDATED);
            }

            fileSystemObjectManager.save(nextDirectoryChild.getDbDirectory(root.getIdForParent()));

            processTree(nextDirectoryChild, migrateDateDTO);
        }
    }

    public void updateDirectories(MigrateDateDTO migrateDateDTO) {
        Map<String,DirectoryNode> sources = new HashMap<>();

        int count = 0;
        for(FileSystemObject nextDirectory : fileSystemObjectManager.findAllByType(FileSystemObjectType.FSO_DIRECTORY)) {
            DirectoryInfo directory = (DirectoryInfo) nextDirectory;

            // Get the directory node for the source.
            DirectoryNode rootOfSource;
            if(sources.containsKey(directory.getParentId().toString())) {
                rootOfSource = sources.get(directory.getParentId().toString());
            } else {
                rootOfSource = new DirectoryNode(null, directory.getParentId());
                sources.put(directory.getParentId().toString(), rootOfSource);
            }

            rootOfSource.addDirectory(directory);
            count++;
        }

        LOG.info("Found {} directories", count);
        LOG.info("Under {} sources", sources.keySet().size());

        for(Map.Entry<String,DirectoryNode> nextEntry : sources.entrySet()) {
            LOG.info("Process Key: {}", nextEntry.getKey());

            processTree(nextEntry.getValue(), migrateDateDTO);
        }

        LOG.info("Completed - new     {}", migrateDateDTO.getCount(MigrateDateDTO.MigrateDataCountType.NEW_DIRECTORIES));
        LOG.info("Completed - updated {}", migrateDateDTO.getCount(MigrateDateDTO.MigrateDataCountType.DIRECTORIES_UPDATED));
        LOG.info("Done");
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
