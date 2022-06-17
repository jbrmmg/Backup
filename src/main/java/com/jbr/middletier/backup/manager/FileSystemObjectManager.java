package com.jbr.middletier.backup.manager;


import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.exception.MissingFileSystemObject;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class FileSystemObjectManager {
    private final FileRepository fileRepository;
    private final DirectoryRepository directoryRepository;
    private final IgnoreFileRepository ignoreFileRepository;
    private final AssociatedFileDataManager associatedFileDataManager;
    private final ImportFileRepository importFileRepository;

    @Autowired
    public FileSystemObjectManager(FileRepository fileRepository,
                                   DirectoryRepository directoryRepository,
                                   IgnoreFileRepository ignoreFileRepository,
                                   AssociatedFileDataManager associatedFileDataManager,
                                   ImportFileRepository importFileRepository) {
        this.fileRepository = fileRepository;
        this.directoryRepository = directoryRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.associatedFileDataManager = associatedFileDataManager;
        this.importFileRepository = importFileRepository;
    }

    @SuppressWarnings("unchecked")
    static <T> Optional<T> copyOf(Optional<? extends T> opt) {
        return (Optional<T>) opt;
    }

    @SuppressWarnings("unchecked")
    static <T> Iterable<T> copyOfList(Iterable<? extends T> list) {
        return (Iterable<T>) list;
    }

    private Optional<FileSystemObject> processFindResult(Optional<FileSystemObject> read, FileSystemObjectId id, boolean failIfMissing) throws MissingFileSystemObject {
        if(!read.isPresent()) {
            if(failIfMissing) {
                throw new MissingFileSystemObject("Not Found", id);
            }
        }

        return read;
    }

    public Iterable<FileSystemObject> findAllByType(FileSystemObjectType type) {
        List<FileSystemObject> empty = new ArrayList<>();

        switch(type) {
            case FSO_FILE:
                return copyOfList(fileRepository.findAllByOrderByIdAsc());
            case FSO_DIRECTORY:
                return copyOfList(directoryRepository.findAllByOrderByIdAsc());
        }

        return empty;
    }

    public void save(FileSystemObject fso) {
        switch(fso.getIdAndType().getType()) {
            case FSO_FILE:
                fileRepository.save((FileInfo) fso);
                break;

            case FSO_DIRECTORY:
                directoryRepository.save((DirectoryInfo) fso);
                break;

            default:
                throw new IllegalStateException("Save except for File and Directory not supported");
        }
    }

    public void delete(FileSystemObject fso) {
        switch(fso.getIdAndType().getType()) {
            case FSO_FILE:
                fileRepository.delete((FileInfo) fso);
                break;

            case FSO_DIRECTORY:
                directoryRepository.delete((DirectoryInfo) fso);
                break;

            default:
                throw new IllegalStateException("Delete except for File and Directory not supported");
        }
    }

    public Optional<FileSystemObject> findFileSystemObject(FileSystemObjectId id, boolean failIfMissing) throws MissingFileSystemObject {
        Optional<FileSystemObject> result = Optional.empty();

        switch(id.getType()) {
            case FSO_IMPORT_SOURCE:
                return processFindResult(copyOf(associatedFileDataManager.internalFindImportSourceByIdIfExists(id.getId())), id, failIfMissing);

            case FSO_DIRECTORY:
                return processFindResult(copyOf(directoryRepository.findById(id.getId())), id, failIfMissing);

            case FSO_FILE:
                return processFindResult(copyOf(fileRepository.findById(id.getId())), id, failIfMissing);

            case FSO_IGNORE_FILE:
                return processFindResult(copyOf(ignoreFileRepository.findById(id.getId())), id, failIfMissing);

            case FSO_IMPORT_FILE:
                return processFindResult(copyOf(importFileRepository.findById(id.getId())), id, failIfMissing);

            case FSO_SOURCE:
                return processFindResult(copyOf(associatedFileDataManager.internalFindSourceByIdIfExists(id.getId())), id, failIfMissing);

            case FSO_IMAGE_FILE:
            case FSO_VIDEO_FILE:
                //TODO
                break;
        }

        if(failIfMissing) {
            throw new MissingFileSystemObject("Unexpected Type", id);
        }

        return result;
    }

    public Iterable<FileSystemObject> findFileSystemObjectByName(String name, FileSystemObjectType type) {
        List<FileSystemObject> empty = new ArrayList<>();

        switch(type) {
            case FSO_FILE:
                return copyOfList(fileRepository.findByName(name));

            case FSO_IMAGE_FILE:
            case FSO_VIDEO_FILE:
                //TODO
                break;
        }

        return empty;
    }

    private void populateFileNamePartsList(FileSystemObject fso, List<FileSystemObject> fileNameParts) throws MissingFileSystemObject {
        if(fso.getParentId() == null) {
            return;
        }

        Optional<FileSystemObject> parent = findFileSystemObject(fso.getParentId(), true);
        if(!parent.isPresent())
            return;

        fileNameParts.add(parent.get());

        populateFileNamePartsList(parent.get(), fileNameParts);
    }

    private File getFileNameFromParts(List<FileSystemObject> nameParts) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(FileSystemObject nextFso : nameParts) {
            if(!first) {
                sb.append("/");
            }
            sb.append(nextFso.getName());

            first = false;
        }

        return new File(sb.toString());
    }

    private File getFileNameFromPartsAtDestination(List<FileSystemObject> nameParts, Source destination) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(FileSystemObject nextFso : nameParts) {
            if(!first) {
                sb.append("/");
            }

            // If this is a source, then use the destination.
            if(nextFso instanceof Source) {
                sb.append(destination.getName());
            } else {
                sb.append(nextFso.getName());
            }

            first = false;
        }

        return new File(sb.toString());
    }

    public File getFile(FileSystemObject fso) throws MissingFileSystemObject {
        List<FileSystemObject> fileNameParts = new ArrayList<>();
        fileNameParts.add(fso);

        populateFileNamePartsList(fso, fileNameParts);

        Collections.reverse(fileNameParts);
        return getFileNameFromParts(fileNameParts);
    }

    public File getFileAtDestination(FileSystemObject fso, Source destination) throws MissingFileSystemObject {
        List<FileSystemObject> fileNameParts = new ArrayList<>();
        fileNameParts.add(fso);

        populateFileNamePartsList(fso, fileNameParts);

        Collections.reverse(fileNameParts);
        return getFileNameFromPartsAtDestination(fileNameParts, destination);
    }

    public DbRoot createDbRoot(Source source) {
        return new DbRoot(source,fileRepository,directoryRepository);
    }
}
