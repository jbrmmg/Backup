package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FileSystemObjectManager {
    private final FileRepository fileRepository;
    private final DirectoryRepository directoryRepository;
    private final IgnoreFileRepository ignoreFileRepository;
    private final SourceRepository sourceRepository;
    private final ImportFileRepository importFileRepository;

    @Autowired
    public FileSystemObjectManager(FileRepository fileRepository, DirectoryRepository directoryRepository, IgnoreFileRepository ignoreFileRepository, SourceRepository sourceRepository, ImportFileRepository importFileRepository) {
        this.fileRepository = fileRepository;
        this.directoryRepository = directoryRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.sourceRepository = sourceRepository;
        this.importFileRepository = importFileRepository;
    }

    public Optional<FileSystemObject> findFileSystemObject(FileSystemObjectId id) {
        Optional<FileSystemObject> result = Optional.empty();

        switch(id.getType()) {
            case FSO_DIRECTORY:
                Optional<DirectoryInfo> directory = directoryRepository.findById(id.getId());
                if(directory.isPresent()) {
                    return Optional.of(directory.get());
                }

            case FSO_FILE:
                Optional<FileInfo> file = fileRepository.findById(id.getId());
                if(file.isPresent()) {
                    //TODO
//                    return Optional.of(file.get());
                }

            case FSO_IGNORE_FILE:
                //TODO
                Optional<IgnoreFile> ignoreFile = ignoreFileRepository.findById(id.getId());
                break;

            case FSO_IMAGE_FILE:
                //TODO
                break;

            case FSO_IMPORT_FILE:
                //TODO
                Optional<ImportFile> importFile = importFileRepository.findById(id.getId());
                break;

            case FSO_SOURCE:
                Optional<Source> source = sourceRepository.findById(id.getId());
                if(source.isPresent()) {
                    return Optional.of(source.get());
                }

            case FSO_IMPORT_SOURCE:
                //TODO
                break;

            case FSO_VIDEO_FILE:
                //TODO
                break;
        }

        return result;
    }
}
