package com.jbr.middletier.backup.manager;


import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.FileInfoDTO;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;

    @Autowired
    public FileSystemObjectManager(FileRepository fileRepository,
                                   DirectoryRepository directoryRepository,
                                   IgnoreFileRepository ignoreFileRepository,
                                   AssociatedFileDataManager associatedFileDataManager,
                                   ImportFileRepository importFileRepository, ModelMapper modelMapper) {
        this.fileRepository = fileRepository;
        this.directoryRepository = directoryRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.associatedFileDataManager = associatedFileDataManager;
        this.importFileRepository = importFileRepository;
        this.modelMapper = modelMapper;
    }

    public FileInfoDTO convertToDTO(FileInfo fileInfo) {
        return modelMapper.map(fileInfo, FileInfoDTO.class);
    }

    @SuppressWarnings("unchecked")
    static <T> Optional<T> copyOf(Optional<? extends T> opt) {
        return (Optional<T>) opt;
    }

    @SuppressWarnings("unchecked")
    static <T> Iterable<T> copyOfList(Iterable<? extends T> list) {
        return (Iterable<T>) list;
    }

    public Iterable<FileSystemObject> findAllByType(FileSystemObjectType type) {
        List<FileSystemObject> empty = new ArrayList<>();

        switch(type) {
            case FSO_FILE:
                return copyOfList(fileRepository.findAllByOrderByIdAsc());
            case FSO_DIRECTORY:
                return copyOfList(directoryRepository.findAllByOrderByIdAsc());
            case FSO_IGNORE_FILE:
                return copyOfList(ignoreFileRepository.findAllByOrderByIdAsc());
            case FSO_IMPORT_FILE:
                return copyOfList(importFileRepository.findAllByOrderByIdAsc());
            default:
                // Nothing to return for the others.
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

            case FSO_IMPORT_FILE:
                importFileRepository.save((ImportFile) fso);
                break;

            case FSO_IGNORE_FILE:
                ignoreFileRepository.save((IgnoreFile) fso);
                break;

            default:
                throw new IllegalStateException("Save not supported for " + fso.getIdAndType().getId());
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

            case FSO_IMPORT_FILE:
                importFileRepository.delete((ImportFile) fso);
                break;

            default:
                throw new IllegalStateException("Delete not supported for " + fso.getIdAndType().getId());
        }
    }

    public void deleteAllFileObjects() {
        fileRepository.deleteAll();
        ignoreFileRepository.deleteAll();
        importFileRepository.deleteAll();

        List<DirectoryInfo> dbDirectories = new ArrayList<>(directoryRepository.findAllByOrderByIdAsc());
        for(DirectoryInfo nextDirectory : dbDirectories) {
            nextDirectory.setParent(null);
            directoryRepository.save(nextDirectory);
        }

        directoryRepository.deleteAll();
    }

    public Optional<FileSystemObject> findFileSystemObject(FileSystemObjectId id) {
        Optional<FileSystemObject> result = Optional.empty();

        if(id == null) {
            return result;
        }

        switch(id.getType()) {
            case FSO_IMPORT_SOURCE:
                return copyOf(associatedFileDataManager.findImportSourceIfExists(id.getId()));

            case FSO_PRE_IMPORT_SOURCE:
                return copyOf(associatedFileDataManager.findPreImportSourceIfExists(id.getId()));

            case FSO_DIRECTORY:
                return copyOf(directoryRepository.findById(id.getId()));

            case FSO_FILE:
                return copyOf(fileRepository.findById(id.getId()));

            case FSO_IGNORE_FILE:
                return copyOf(ignoreFileRepository.findById(id.getId()));

            case FSO_IMPORT_FILE:
                return copyOf(importFileRepository.findById(id.getId()));

            case FSO_SOURCE:
                return copyOf(associatedFileDataManager.findSourceIfExists(id.getId()));

            default:
                // Nothing else is supported
        }

        return result;
    }

    public Iterable<FileSystemObject> findFileSystemObjectByName(String name, FileSystemObjectType type) {
        List<FileSystemObject> empty = new ArrayList<>();

        switch (type) {
            case FSO_FILE:
                return copyOfList(fileRepository.findByName(name));
            case FSO_IMPORT_FILE:
                return copyOfList(importFileRepository.findByName(name));
            default:
                // Nothing else supported for this method.
        }

        return empty;
    }

    private void populateFileNamePartsList(FileSystemObject fso, List<FileSystemObject> fileNameParts) {
        Optional<FileSystemObject> parent = findFileSystemObject(fso.getParentId().orElse(null));
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

    public File getFile(FileSystemObject fso) {
        List<FileSystemObject> fileNameParts = new ArrayList<>();
        fileNameParts.add(fso);

        populateFileNamePartsList(fso, fileNameParts);

        Collections.reverse(fileNameParts);
        return getFileNameFromParts(fileNameParts);
    }

    public File getFileAtDestination(FileSystemObject fso, Source destination) {
        List<FileSystemObject> fileNameParts = new ArrayList<>();
        fileNameParts.add(fso);

        populateFileNamePartsList(fso, fileNameParts);

        Collections.reverse(fileNameParts);
        return getFileNameFromPartsAtDestination(fileNameParts, destination);
    }

    public DbRoot createDbRoot(Source source) {
        return new DbRoot(source,fileRepository,directoryRepository);
    }

    private void loadFilesByParent(int id, List<FileInfo> files) {
        for(FileInfo nextFile: fileRepository.findByParentId(id)) {
            files.add(nextFile);
        }
    }

    public void loadByParent(int id, List<DirectoryInfo> directories, List<FileInfo> files) {
        loadFilesByParent(id,files);

        for(DirectoryInfo next: directoryRepository.findByParentId(id)) {
            directories.add(next);
            loadByParent(next.getIdAndType().getId(), directories, files);
        }
    }

    public void loadImmediateByParent(int id, List<DirectoryInfo> directories, List<FileInfo> files) {
        loadFilesByParent(id,files);

        directories.addAll(directoryRepository.findByParentId(id));
    }
}
