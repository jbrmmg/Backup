package com.jbr.middletier.backup.manager;


import com.jbr.middletier.backup.control.FileController;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.FileInfoDTO;
import com.jbr.middletier.backup.dto.PrintSizeDTO;
import com.jbr.middletier.backup.dto.SelectedPrintDTO;
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
    private final PrintRepository printRepository;
    private final PrintManager printManager;

    @Autowired
    public FileSystemObjectManager(FileRepository fileRepository,
                                   DirectoryRepository directoryRepository,
                                   IgnoreFileRepository ignoreFileRepository,
                                   AssociatedFileDataManager associatedFileDataManager,
                                   ImportFileRepository importFileRepository, ModelMapper modelMapper,
                                   PrintRepository printRepository,
                                   PrintManager printManager) {
        LOG.trace("FSO CTOR");

        this.fileRepository = fileRepository;
        this.directoryRepository = directoryRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.associatedFileDataManager = associatedFileDataManager;
        this.importFileRepository = importFileRepository;
        this.modelMapper = modelMapper;
        this.printRepository = printRepository;
        this.printManager = printManager;
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

    public Integer select(Integer id) {
        // Find the file.
        Optional<FileInfo> file = fileRepository.findById(id);

        if(file.isPresent()) {
            file.get().setFlags("P");
            fileRepository.save(file.get());

            // Create a print (default size = 12)
            Print newPrint = new Print();
            PrintId newPrintId = new PrintId();
            newPrint.setId(newPrintId);
            newPrint.getId().setFileId(id);
            newPrint.getId().setSizeId(12);
            newPrint.setBorder(false);
            newPrint.setBlackWhite(false);

            printRepository.save(newPrint);

            return id;
        }

        return null;
    }

    private void removePrintRow(int fileId) {
        for(Print next : printRepository.findAll()) {
            if(next.getId().getFileId() == fileId) {
                printRepository.delete(next);
                break;
            }
        }
    }

    public Integer unselect(Integer id) {
        // Find the file.
        Optional<FileInfo> file = fileRepository.findById(id);

        if(file.isPresent()) {
            file.get().setFlags(null);
            fileRepository.save(file.get());

            // If any print row exists remove it.
            removePrintRow(id);

            return id;
        }

        return null;
    }

    public List<SelectedPrintDTO> getPrints() {
        List<SelectedPrintDTO> result = new ArrayList<>();

        // Get the print rows.
        for(Print next : printRepository.findAll()) {
            SelectedPrintDTO nextPrint = new SelectedPrintDTO();
            nextPrint.setFileId(next.getId().getFileId());
            nextPrint.setSizeId(next.getId().getSizeId());
            nextPrint.setBorder(next.getBorder());
            nextPrint.setBlackWhite(next.getBlackWhite());

            for(PrintSizeDTO nextSize : printManager.getPrintSizes()) {
                if(nextPrint.getSizeId() == nextSize.getId()) {
                    nextPrint.setSizeName(nextSize.getName());
                }
            }

            Optional<FileInfo> file = fileRepository.findById(next.getId().getFileId());
            if(file.isPresent()) {
                nextPrint.setFileName(file.get().getName());
            }

            result.add(nextPrint);
        }

        // Check to see if any files have a P flag (legacy).
        for(FileInfo next : fileRepository.findByFlagsContaining("P")) {
            // Is this already in the list?
            boolean already = false;
            for(SelectedPrintDTO existing : result) {
                if(existing.getFileId() == next.getIdAndType().getId()) {
                    already = true;
                    break;
                }
            }

            if(!already) {
                SelectedPrintDTO nextPrint = new SelectedPrintDTO();
                nextPrint.setFileId(next.getIdAndType().getId());
                nextPrint.setSizeName("6x4 in");
                nextPrint.setSizeId(12);
                nextPrint.setFileName(next.getName());
                nextPrint.setBorder(false);
                nextPrint.setBlackWhite(false);

                result.add(nextPrint);
            }
        }

        return result;
    }

    public Integer updatePrint(SelectedPrintDTO print) {
        // Update the print
        LOG.info("Update print details - {} {} {} {} {}", print.getFileId(), print.getSizeId(), print.getSizeName(), print.getBlackWhite(), print.getBorder());

        // Delete if exists.
        removePrintRow(print.getFileId());

        // Create a print.
        Print newPrint = new Print();
        PrintId newPrintId = new PrintId();
        newPrintId.setFileId(print.getFileId());
        newPrintId.setSizeId(print.getSizeId());
        newPrint.setId(newPrintId);
        newPrint.setBlackWhite(print.getBlackWhite());
        newPrint.setBorder(print.getBorder());

        printRepository.save(newPrint);

        return print.getFileId();
    }

    public List<Integer> deletePrints() {
        List<Integer> result = new ArrayList<>();

        for(FileInfo next : fileRepository.findByFlagsContaining("P")) {
            next.setFlags(null);

            fileRepository.save(next);
        }

        return result;
    }

    public void gatherList() {
        // Find the P files.
        for(FileInfo next : fileRepository.findByFlagsContaining("P")) {
            File printFile = getFile(next);
            LOG.info("cp " + printFile.getAbsolutePath() + " /media/jason/6263-3935/" + printFile.getName());
        }
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
}
