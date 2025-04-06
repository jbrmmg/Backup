package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ImportSourceManager {
    private static final Logger LOG = LoggerFactory.getLogger(ImportSourceManager.class);

    private final AssociatedFileDataManager associatedFileDataManager;

    @Autowired
    public ImportSourceManager(AssociatedFileDataManager associatedFileDataManager) {
        this.associatedFileDataManager = associatedFileDataManager;
    }

    private List<Source> getSourceIterator(FileSystemObjectType sourceType) {
        List<Source> result = new ArrayList<>();

        switch (sourceType) {
            case FSO_PRE_IMPORT_SOURCE -> result.addAll(associatedFileDataManager.findAllPreImportSource());
            case FSO_IMPORT_SOURCE -> result.addAll(associatedFileDataManager.findAllImportSource());
            case FSO_POST_IMPORT_SOURCE -> result.addAll(associatedFileDataManager.findAllPostImportSource());
            default -> {
                // Just return empty.
                return result;
            }
        }

        return result;
    }

    public Optional<Source> findSource(FileSystemObjectType sourceType) {
        Optional<Source> result = Optional.empty();

        int count = 0;
        for(Source nextSource : getSourceIterator(sourceType)) {
            result = Optional.of(nextSource);

            if(count > 0) {
                LOG.warn("Too many sources specified, do not import.");
                return Optional.empty();
            }

            count++;
        }

        return result;
    }

    public File getPreImportDirectory() {
        Optional<Source> preImportSource = findSource(FileSystemObjectType.FSO_PRE_IMPORT_SOURCE);
        if (preImportSource.isEmpty()) {
            return null;
        }

        File preImportDirectory = new File(preImportSource.get().getPath());

        if(!preImportDirectory.exists()) {
            return null;
        }

        return preImportDirectory;
    }

    public File getImportDirectory() {
        Optional<Source> importSource = findSource(FileSystemObjectType.FSO_IMPORT_SOURCE);
        if (importSource.isEmpty()) {
            return null;
        }

        File importDirectory = new File(importSource.get().getPath());

        if(!importDirectory.exists()) {
            return null;
        }

        return importDirectory;
    }

    public File getPostImportDirectory() {
        Optional<Source> postImportSource = findSource(FileSystemObjectType.FSO_POST_IMPORT_SOURCE);
        if (postImportSource.isEmpty()) {
            return null;
        }

        File postImportDirectory = new File(postImportSource.get().getPath());

        if(!postImportDirectory.exists()) {
            return null;
        }

        return postImportDirectory;
    }
}
