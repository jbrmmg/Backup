package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.FileInfoDTO;
import com.jbr.middletier.backup.dto.FileInfoExtra;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class FileSystemObjectManager {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemObjectManager.class);

    private final FileRepository fileRepository;
    private final DirectoryRepository directoryRepository;
    private final IgnoreFileRepository ignoreFileRepository;
    private final AssociatedFileDataManager associatedFileDataManager;
    private final ImportFileRepository importFileRepository;
    private final ModelMapper modelMapper;
    private final LabelManager labelManager;

    @Autowired
    public FileSystemObjectManager(FileRepository fileRepository,
                                   DirectoryRepository directoryRepository,
                                   IgnoreFileRepository ignoreFileRepository,
                                   AssociatedFileDataManager associatedFileDataManager,
                                   ImportFileRepository importFileRepository,
                                   ModelMapper modelMapper,
                                   LabelManager labelManager) {
        LOG.trace("FSO CTOR");

        this.fileRepository = fileRepository;
        this.directoryRepository = directoryRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.associatedFileDataManager = associatedFileDataManager;
        this.importFileRepository = importFileRepository;
        this.modelMapper = modelMapper;
        this.labelManager = labelManager;
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

        return switch (type) {
            case FSO_FILE -> copyOfList(fileRepository.findAllByOrderByIdAsc());
            case FSO_DIRECTORY -> copyOfList(directoryRepository.findAllByOrderByIdAsc());
            case FSO_IGNORE_FILE -> copyOfList(ignoreFileRepository.findAllByOrderByIdAsc());
            case FSO_IMPORT_FILE -> copyOfList(importFileRepository.findAllByOrderByIdAsc());
            default ->
                // Nothing to return for the others.
                empty;
        };

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

        return switch (id.getType()) {
            case FSO_IMPORT_SOURCE -> copyOf(associatedFileDataManager.findImportSourceIfExists(id.getId()));
            case FSO_PRE_IMPORT_SOURCE -> copyOf(associatedFileDataManager.findPreImportSourceIfExists(id.getId()));
            case FSO_DIRECTORY -> copyOf(directoryRepository.findById(id.getId()));
            case FSO_FILE -> copyOf(fileRepository.findById(id.getId()));
            case FSO_IGNORE_FILE -> copyOf(ignoreFileRepository.findById(id.getId()));
            case FSO_IMPORT_FILE -> copyOf(importFileRepository.findById(id.getId()));
            case FSO_SOURCE -> copyOf(associatedFileDataManager.findSourceIfExists(id.getId()));
            default ->
                // Nothing else is supported
                    result;
        };

    }

    public Iterable<FileSystemObject> findFileSystemObjectByName(String name, FileSystemObjectType type) {
        List<FileSystemObject> empty = new ArrayList<>();

        return switch (type) {
            case FSO_FILE -> copyOfList(fileRepository.findByName(name));
            case FSO_IMPORT_FILE -> copyOfList(importFileRepository.findByName(name));
            default ->
                // Nothing else supported for this method.
                    empty;
        };

    }

    private void populateFileNamePartsList(FileSystemObject fso, List<FileSystemObject> fileNameParts) {
        Optional<FileSystemObject> parent = findFileSystemObject(fso.getParentId().orElse(null));
        if(parent.isEmpty())
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

    public void setFileExpiry(FileSystemObjectId id, LocalDateTime expiry) {
        // This action is only valid on files.
        if(id.getType() != FileSystemObjectType.FSO_FILE) {
            return;
        }

        Optional<FileInfo> file = fileRepository.findById(id.getId());
        if(file.isEmpty()) {
            return;
        }

        file.get().setExpiry(expiry);
        fileRepository.save(file.get());
    }

    public FileInfoExtra getFileExtra(Integer id) throws InvalidFileIdException {
        Optional<FileSystemObject> file = findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(id);
        }

        FileInfo originalFile = (FileInfo)file.get();
        File associatedFile = getFile(originalFile);
        FileInfoExtra result = new FileInfoExtra(originalFile,associatedFile.getPath(),associatedFile.getPath(),associatedFile.getParent());

        // Are there backups of this file?
        Iterable<FileSystemObject> sameName = findFileSystemObjectByName(file.get().getName(), FileSystemObjectType.FSO_FILE);

        for(FileSystemObject nextSameName: sameName) {
            if(nextSameName.getIdAndType().equals(file.get().getIdAndType()) || !(nextSameName instanceof FileInfo nextFile) ) {
                continue;
            }

            if(nextFile.getSize().equals(originalFile.getSize()) && nextFile.getMD5().compare(originalFile.getMD5(),true)) {
                associatedFile = getFile(nextFile);
                result.addFile(nextFile,associatedFile.getPath(),associatedFile.getPath(),associatedFile.getParent());
            }
        }

        // Get any labels.
        for(String nextLabel : labelManager.getLabelsForFile(file.get().getIdAndType())) {
            result.addLabel(nextLabel);
        }

        return result;
    }
}
