package com.jbr.middletier.backup.summary;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.ImportSource;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dto.ImportSourceDTO;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@SuppressWarnings("unused")
public class Summary {
    private static final Logger LOG = LoggerFactory.getLogger(Summary.class);

    @Getter
    boolean valid;
    @Getter
    Date validAt;
    List<SourceDTO> sources;
    @Getter
    String version;

    private record SummaryInitializer(Summary instance, AssociatedFileDataManager associatedFileDataManager,
                                      FileSystemObjectManager fileSystemObjectManager,
                                      ApplicationProperties applicationProperties) implements Runnable {

        private void processNextSource(SourceDTO nextSourceDTO) {
            // Check to see if there is an import source.
            Optional<ImportSource> importSource = associatedFileDataManager.findImportSourceIfExists(nextSourceDTO.getId());
            if (importSource.isPresent()) {
                ImportSourceDTO importSourceDTO = associatedFileDataManager.convertToDTO(importSource.get());
                nextSourceDTO = importSourceDTO;
                instance.sources.add(importSourceDTO);
            } else {
                instance.sources.add(nextSourceDTO);
            }

            List<FileInfo> files = new ArrayList<>();
            List<DirectoryInfo> directories = new ArrayList<>();
            fileSystemObjectManager.loadByParent(nextSourceDTO.getId(), directories, files);

            for (DirectoryInfo directory : directories) nextSourceDTO.incrementDirectoryCount();
            for (FileInfo file : files) {
                nextSourceDTO.incrementFileCount();
                if (file.getSize() != null) {
                    nextSourceDTO.increaseFileSize(file.getSize());
                }
            }
        }

        @Override
        public void run() {
            try {
                instance.sources = new ArrayList<>();

                if (Boolean.TRUE.equals(applicationProperties.getSummaryEnabled())) {
                    // Initialise the summary object.
                    for (Source nextSource : associatedFileDataManager.findAllSource()) {
                        processNextSource(associatedFileDataManager.convertToDTO(nextSource));
                    }

                    // Set the object to valid.
                    instance.validAt = new Date();
                    instance.version = applicationProperties.getVersion();
                    instance.valid = true;
                }
            } catch (Exception e) {
                LOG.error("Failed to get the summary: ", e);
            }
        }
    }

    private static Summary instance = null;

    public static Summary getInstance(AssociatedFileDataManager associatedFileDataManager, FileSystemObjectManager fileSystemObjectManager, ApplicationProperties applicationProperties) {
        if(instance != null) {
            return instance;
        }

        instance = new Summary();

        SummaryInitializer initializer = new SummaryInitializer(instance, associatedFileDataManager, fileSystemObjectManager,applicationProperties);
        new Thread(initializer).start();

        return instance;
    }

    public static void forceInstance(AssociatedFileDataManager associatedFileDataManager, FileSystemObjectManager fileSystemObjectManager, ApplicationProperties applicationProperties) {
        instance = null;

        instance = new Summary();
        SummaryInitializer initializer = new SummaryInitializer(instance, associatedFileDataManager, fileSystemObjectManager, applicationProperties);
        initializer.run();
    }

    private Summary() {
        LOG.info("Initialise the summary data.");
        this.valid = false;
    }

    public List<SourceDTO> getSources() {
        return Collections.unmodifiableList(this.sources);
    }
}
