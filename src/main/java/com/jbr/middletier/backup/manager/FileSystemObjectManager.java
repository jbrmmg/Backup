package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.FileInfoDTO;
import com.jbr.middletier.backup.dto.FileInfoExtra;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.filetree.database.DbRoot;
import com.jbr.middletier.backup.util.FileSearch;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class FileSystemObjectManager {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemObjectManager.class);

    private final FileRepository fileRepository;
    private final MetaDataRepository metaDataRepository;
    private final DirectoryRepository directoryRepository;
    private final IgnoreFileRepository ignoreFileRepository;
    private final AssociatedFileDataManager associatedFileDataManager;
    private final ImportFileRepository importFileRepository;
    private final ModelMapper modelMapper;
    private final LabelManager labelManager;
    private final FileSystem fileSystem;

    @Autowired
    public FileSystemObjectManager(FileRepository fileRepository,
                                   MetaDataRepository metaDataRepository,
                                   DirectoryRepository directoryRepository,
                                   IgnoreFileRepository ignoreFileRepository,
                                   AssociatedFileDataManager associatedFileDataManager,
                                   ImportFileRepository importFileRepository,
                                   ModelMapper modelMapper,
                                   LabelManager labelManager,
                                   FileSystem fileSystem) {
        LOG.trace("FSO CTOR");

        this.fileRepository = fileRepository;
        this.metaDataRepository = metaDataRepository;
        this.directoryRepository = directoryRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.associatedFileDataManager = associatedFileDataManager;
        this.importFileRepository = importFileRepository;
        this.modelMapper = modelMapper;
        this.labelManager = labelManager;
        this.fileSystem = fileSystem;
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
            case FSO_POST_IMPORT_SOURCE -> copyOf(associatedFileDataManager.findPostImportSourceIfExists(id.getId()));
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

    private void addFileToResult(FileInfo fileInfo, List<String> result) {
        File file = getFile(fileInfo);

        if(file.getName().equalsIgnoreCase(file.getPath())) {
            result.add(file.getName() + "[" + fileInfo.getIdAndType().getType().getTypeName() + "]");
            return;
        }

        result.add(file.getPath());
    }

    private void customFileAction() {
        try {
            LOG.info("Custom action started - acts on the master source.");

            int count = 0;
            List<FileInfo> files = new ArrayList<>();
            List<DirectoryInfo> directories = new ArrayList<>();
            loadByParent(4, directories, files);

            for(FileInfo next : files) {
                LOG.info("{} - {}", next.getName(), next.getIdAndType().getType().getTypeName());
                count++;
            }

            LOG.info("Found {} files", count);
            LOG.warn("Custom action completed");
        } catch(Exception e) {
            LOG.warn("Custom action stopped due to exception", e);
        }
    }

    public List<String> findFiles(String search) {
        // Special actions triggered by
        if(search.equals("!!!!")) {
            customFileAction();
            return new ArrayList<>();
        }

        FileSearch fileSearch = new FileSearch(search);
        List<String> result = new ArrayList<>();

        // Perform the required search.
        switch(fileSearch.getSearchType()) {
            case MD5 -> {
                for(FileInfo fileInfo : fileRepository.findByMd5(fileSearch.getSearch())) {
                    addFileToResult(fileInfo, result);
                }
            }
            case NAME -> {
                for(FileInfo fileInfo : fileRepository.findByName(fileSearch.getSearch())) {
                    addFileToResult(fileInfo, result);
                }
            }
            default -> {
                // Do nothing - just return an empty search.
            }
        }

        return result;
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

    private FileSystemObject getParent(FileSystemObject file) {
        Optional<FileSystemObject> parent = findFileSystemObject(file.getParentId().orElse(null));

        // Return the parent or null.
        return parent.orElse(null);
    }

    private Source getSource(FileSystemObject file) {
        Optional<FileSystemObject> parent = findFileSystemObject(file.getParentId().orElse(null));

        while(parent.isPresent()) {
            if(parent.get() instanceof Source source) {
                return source;
            }

            parent = findFileSystemObject(parent.get().getParentId().orElse(null));
        }

        return null;
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

    public File getImageFromVideoFile(FileSystemObject fso) {
        try {
            File file = getFile(fso);

            return this.fileSystem.getImageFileFromVideoFile(file);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch(Exception e) {
            LOG.warn("Unable to get image from video file {}", fso.getName(), e);
        }

        return null;
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

    private Optional<MetaData> getFileMetaData(boolean useMetaData, FileInfo fileInfo, int id, File associatedFile) {
        Optional<MetaData> result = Optional.empty();

        if(useMetaData && fileInfo.getClassification().getCheckMetaData()) {
            // Is there metadata?
            result = findMetaDataForFile(fileInfo);

            if(result.isEmpty()) {
                // Get the metadata.
                Optional<FileSystemImageData> fileMetaData = this.fileSystem.readImageMetaData(associatedFile);

                if(fileMetaData.isPresent()) {
                    result = Optional.of(new MetaData(id, fileMetaData.get()));

                    // Save the metadata.
                    metaDataRepository.save(result.get());
                }
            }
        }

        return result;
    }

    private boolean updateClassification(FileInfo fileInfo, int id) {
        if(fileInfo.getClassification() == null) {
            Optional<Classification> classification = associatedFileDataManager.classifyFile(fileInfo);

            if(classification.isPresent()) {
                fileInfo.setClassification(classification.get());

                fileRepository.save(fileInfo);

                return true;
            }

            LOG.info("{} there is no classification for this file, therefore nothing further can be done", id);
        } else {
            return true;
        }

        return false;
    }

    private void updateMD5(FileInfo fileInfo, int id, File associatedFile) {
        if(fileInfo.getMd5().isPresent()) {
            // See if the MD5 can be refreshed.
            Optional<MD5> md5 = this.fileSystem.getFileMD5(associatedFile.toPath(),id);
            long size = associatedFile.length();

            if(md5.isPresent()) {
                fileInfo.setMd5(md5.get());
                fileInfo.setSize(size);

                fileRepository.save(fileInfo);
            }
        }
    }

    public FileInfoExtra refreshFileData(Integer id) throws InvalidFileIdException {
        Optional<FileSystemObject> file = findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(id);
        }

        // Get the file info.
        if(!(file.get() instanceof FileInfo fileInfo)) {
            // Nothing to do as it's not the right type.
            LOG.info("{} is not a File, no updates required.", id);
            return null;
        }

        File associatedFile = getFile(fileInfo);

        // Does the source of this file use metadata?
        FileSystemObject parent = getParent(file.get());
        Source fileSource = getSource(file.get());

        if(fileSource == null) {
            LOG.info("{} is not a File, no updates required.", id);
            return null;
        }

        boolean useMetaData = fileSource.getGatherMetaData();

        // Does the file have a classification? If not, see if it can be updated and if it's still not present, then nothing further can be done
        if(!updateClassification(fileInfo, id)) {
            return new FileInfoExtra(fileInfo,null,associatedFile.getParent(), associatedFile.getPath(), associatedFile.getParent());
        }

        // Does the file require an MD5 and is it missing?
        updateMD5(fileInfo, id, associatedFile);

        // Does the file require metadata and is it missing?
        Optional<MetaData> metaData = getFileMetaData(useMetaData, fileInfo, id, associatedFile);

        // Create the FileInfoExtra
        FileInfoExtra fileInfoExtra = new FileInfoExtra ( fileInfo,
                metaData.orElse(null),
                associatedFile.getParent(),
                associatedFile.getPath(),
                associatedFile.getParent());

        long size = fileInfoExtra.getFile().getSize();
        Iterable<FileSystemObject> sameName = findFileSystemObjectByName(fileInfoExtra.getFile().getName(), FileSystemObjectType.FSO_FILE);
        for (FileSystemObject nextSameName: sameName) {
            if(nextSameName.getIdAndType().getId().equals(fileInfoExtra.getFile().getId()) || !(nextSameName instanceof FileInfo nextFile) ) {
                continue;
            }

            // Get the size, parent and MD5 if available.
            long nextSize = nextFile.getSize();
            FileSystemObject nextParent = getParent(nextFile);

            if(size == nextSize && nextParent != null && parent != null && nextParent.getName().equals(parent.getName())) {
                File associatedBackupFile = getFile(nextFile);
                fileInfoExtra.addFile(nextFile,associatedBackupFile.getPath(),associatedBackupFile.getPath(),associatedBackupFile.getParent());

                // Update the classification.
                Optional<Classification> classification = associatedFileDataManager.classifyFile(nextFile);
                classification.ifPresent(nextFile::setClassification);

                Optional<MD5> newNextMD5 = fileSystem.getFileMD5(associatedFile.toPath(), nextFile.getIdAndType().getId());

                newNextMD5.ifPresent(nextFile::setMd5);
                fileRepository.save(nextFile);
            }
        }

        return fileInfoExtra;
    }

    public FileInfoExtra getFileExtra(Integer id) throws InvalidFileIdException {
        Optional<FileSystemObject> file = findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(id);
        }

        Optional<MetaData> metaData = metaDataRepository.findById(id);

        FileInfo originalFile = (FileInfo)file.get();
        File associatedFile = getFile(originalFile);
        FileInfoExtra result = new FileInfoExtra(originalFile, metaData.orElse(null), associatedFile.getPath(), associatedFile.getPath(), associatedFile.getParent());

        // Check for backups
        long size = result.getFile().getSize();
        Optional<MD5> md5 = result.getFile().getMd5Optional();

        Iterable<FileSystemObject> sameName = findFileSystemObjectByName(result.getFile().getName(), FileSystemObjectType.FSO_FILE);
        for (FileSystemObject nextSameName: sameName) {
            if(nextSameName.getIdAndType().getId().equals(result.getFile().getId()) || !(nextSameName instanceof FileInfo nextFile) ) {
                continue;
            }

            // Get the size, parent and MD5 if available.
            long nextSize = nextFile.getSize();
            Optional<MD5> nextMD5 = nextFile.getMd5();
            FileSystemObject nextParent = getParent(nextFile);

            if(size == nextSize && nextParent != null && associatedFile.getParent() != null && nextParent.getName().equals(associatedFile.getParent()) && md5.equals(nextMD5)) {
                File associatedBackupFile = getFile(nextFile);
                result.addFile(nextFile,associatedBackupFile.getPath(),associatedBackupFile.getPath(),associatedBackupFile.getParent());
            }
        }

        // Get labels.
        for(String nextLabel : labelManager.getLabelsForFile(originalFile.getIdAndType())) {
            result.addLabel(nextLabel);
        }

        return result;
    }

    public Optional<MetaData> findMetaDataForFile(FileInfo file) {
        // Return the metadata for the i
        return this.metaDataRepository.findById(file.getIdAndType().getId());
    }

    public void saveMetaData(MetaData metaData) {
        // Save the metadata.
        this.metaDataRepository.save(metaData);
    }

    public void updateMetaData(MetaData metaData) {
        // Save the metadata.
        this.metaDataRepository.save(metaData);
    }

    public void deleteMetaData(MetaData metaData) {
        // Save the metadata.
        this.metaDataRepository.delete(metaData);
    }
}
