package com.jbr.middletier.backup.summary;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileInfo;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Summary {
    private static final Logger LOG = LoggerFactory.getLogger(Summary.class);

    boolean valid;
    Date validAt;
    List<SourceDTO> sources;

    private static class SummaryInitialiser implements Runnable {
        private final AssociatedFileDataManager associatedFileDataManager;
        private final DirectoryRepository directoryRepository;
        private final FileRepository fileRepository;
        private final Summary instance;

        public SummaryInitialiser(Summary instance, AssociatedFileDataManager associatedFileDataManager, DirectoryRepository directoryRepository, FileRepository fileRepository) {
            this.associatedFileDataManager = associatedFileDataManager;
            this.directoryRepository = directoryRepository;
            this.fileRepository = fileRepository;
            this.instance = instance;
        }

        @Override
        public void run() {
            try {
                instance.sources = new ArrayList<>();

                // Initialise the summary object.
                for (Source nextSource : associatedFileDataManager.internalFindAllSource()) {
                    SourceDTO nextSourceDTO = nextSource.getSourceDTO();
                    instance.sources.add(nextSourceDTO);

                    if(true)
                        throw new IllegalStateException("Fix this");
//                    for (DirectoryInfo nextDirectory : this.directoryRepository.findBySource(nextSource)) {
                        nextSourceDTO.incrementDirectoryCount();

//                        for (FileInfo nextFile : this.fileRepository.findByDirectoryInfo(nextDirectory)) {
                            nextSourceDTO.incrementFileCount();
  //                          if (nextFile.getSize() != null) {
    //                            nextSourceDTO.increaseFileSize(nextFile.getSize());
      //                      }
                        }
        //            }
          //      }

                // Set the object to valid.
                instance.validAt = new Date();
                instance.valid = true;
            } catch(Exception e) {
                LOG.error("Failed to get the summary: ",e);
            }
        }
    }

    private static Summary instance = null;

    public static Summary getInstance(AssociatedFileDataManager associatedFileDataManager, DirectoryRepository directoryRepository, FileRepository fileRepository) {
        if(instance != null) {
            return instance;
        }

        instance = new Summary();

        SummaryInitialiser initialiser = new SummaryInitialiser(instance, associatedFileDataManager, directoryRepository, fileRepository);
        new Thread(initialiser).start();

        return instance;
    }

    private Summary() {
        LOG.info("Initialise the summary data.");
        this.valid = false;
    }

    public Date getValidAt() {
        return validAt;
    }

    public boolean isValid() {
        return this.valid;
    }

    public List<SourceDTO> getSources() {
        return Collections.unmodifiableList(this.sources);
    }
}
