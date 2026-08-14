package com.jbr.middletier.backup;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import com.jbr.middletier.backup.manager.LabelManager;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestFsoExtra {
    private FileSystemObjectManager createManager(FileRepository fileRepository,
                                                  MetaDataRepository metaDataRepository,
                                                  DirectoryRepository directoryRepository,
                                                  IgnoreFileRepository ignoreFileRepository,
                                                  AssociatedFileDataManager associatedFileDataManager,
                                                  ImportFileRepository importFileRepository,
                                                  ModelMapper modelMapper,
                                                  LabelManager labelManager,
                                                  FileSystem fileSystem) {
        CustomMetaDataRepository customMetaDataRepository = mock(CustomMetaDataRepository.class);
        return new FileSystemObjectManager(fileRepository,
                metaDataRepository,
                customMetaDataRepository,
                directoryRepository,
                ignoreFileRepository,
                associatedFileDataManager,
                importFileRepository,
                modelMapper,
                labelManager,
                fileSystem);
    }

    @Test
    void findByTypeDirectory() {
        ModelMapper modelMapper = mock(ModelMapper.class);
        FileRepository fileRepository = mock(FileRepository.class);
        MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        LabelManager labelManager = mock(LabelManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        DirectoryInfo directory = new DirectoryInfo();
        directory.setName("Test");
        List<DirectoryInfo> testList = new ArrayList<>();
        testList.add(directory);
        when(directoryRepository.findAllByOrderByIdAsc()).thenReturn(testList);

        FileSystemObjectManager manager = createManager(fileRepository, metaDataRepository, directoryRepository,
                ignoreFileRepository, associatedFileDataManager, importFileRepository, modelMapper, labelManager, fileSystem);

        AtomicInteger count = new AtomicInteger(0);
        manager.findAllByType(FileSystemObjectType.FSO_DIRECTORY).forEach(nextDirectory -> {
            assertEquals("Test", nextDirectory.getName());
            int i = count.get();
            count.set(++i);
        });
        assertEquals(1, count.get());
    }

    @Test
    void findByTypeDirectory2() {
        ModelMapper modelMapper = mock(ModelMapper.class);
        FileRepository fileRepository = mock(FileRepository.class);
        MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        LabelManager labelManager = mock(LabelManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        DirectoryInfo directory = new DirectoryInfo();
        directory.setName("Test");
        List<DirectoryInfo> testList = new ArrayList<>();
        testList.add(directory);
        when(directoryRepository.findAllByOrderByIdAsc()).thenReturn(testList);

        FileSystemObjectManager manager = createManager(fileRepository, metaDataRepository, directoryRepository,
                ignoreFileRepository, associatedFileDataManager, importFileRepository, modelMapper, labelManager, fileSystem);

        AtomicInteger count = new AtomicInteger(0);
        manager.findAllByType(FileSystemObjectType.FSO_SOURCE).forEach(nextDirectory -> {
            int i = count.get();
            count.set(++i);
        });
        assertEquals(0, count.get());
    }

    @Test
    void fsoSaveUnsupported() {
        ModelMapper modelMapper = mock(ModelMapper.class);
        FileRepository fileRepository = mock(FileRepository.class);
        MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        LabelManager labelManager = mock(LabelManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        FileSystemObject fso = mock(FileSystemObject.class);
        when(fso.getIdAndType()).thenReturn(new FileSystemObjectId(0, FileSystemObjectType.FSO_SOURCE));

        FileSystemObjectManager manager = createManager(fileRepository, metaDataRepository, directoryRepository,
                ignoreFileRepository, associatedFileDataManager, importFileRepository, modelMapper, labelManager, fileSystem);

        try {
            manager.save(fso);
            fail();
        } catch(IllegalStateException e) {
            assertEquals("Save not supported for 0", e.getMessage());
        }
    }

    @Test
    void fsoDeleteUnsupported() {
        ModelMapper modelMapper = mock(ModelMapper.class);
        FileRepository fileRepository = mock(FileRepository.class);
        MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        LabelManager labelManager = mock(LabelManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        FileSystemObject fso = mock(FileSystemObject.class);
        when(fso.getIdAndType()).thenReturn(new FileSystemObjectId(0, FileSystemObjectType.FSO_SOURCE));

        FileSystemObjectManager manager = createManager(fileRepository, metaDataRepository, directoryRepository,
                ignoreFileRepository, associatedFileDataManager, importFileRepository, modelMapper, labelManager, fileSystem);

        try {
            manager.delete(fso);
            fail();
        } catch(IllegalStateException e) {
            assertEquals("Delete not supported for 0", e.getMessage());
        }
    }

    @Test
    void fsoFind1() {
        ModelMapper modelMapper = mock(ModelMapper.class);
        FileRepository fileRepository = mock(FileRepository.class);
        MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        LabelManager labelManager = mock(LabelManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        when(ignoreFileRepository.findById(1)).thenReturn(Optional.empty());

        FileSystemObjectManager manager = createManager(fileRepository, metaDataRepository, directoryRepository,
                ignoreFileRepository, associatedFileDataManager, importFileRepository, modelMapper, labelManager, fileSystem);

        Optional<FileSystemObject> result = manager.findFileSystemObject(new FileSystemObjectId(1, FileSystemObjectType.FSO_IGNORE_FILE));
        assertFalse(result.isPresent());
    }

    @Test
    void fsoFind2() {
        ModelMapper modelMapper = mock(ModelMapper.class);
        FileRepository fileRepository = mock(FileRepository.class);
        MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        LabelManager labelManager = mock(LabelManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        when(importFileRepository.findById(1)).thenReturn(Optional.empty());

        FileSystemObjectManager manager = createManager(fileRepository, metaDataRepository, directoryRepository,
                ignoreFileRepository, associatedFileDataManager, importFileRepository, modelMapper, labelManager, fileSystem);

        Optional<FileSystemObject> result = manager.findFileSystemObject(new FileSystemObjectId(1, FileSystemObjectType.FSO_IMPORT_FILE));
        assertFalse(result.isPresent());
    }

    @Test
    void fsoFind3() {
        ModelMapper modelMapper = mock(ModelMapper.class);
        FileRepository fileRepository = mock(FileRepository.class);
        MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        LabelManager labelManager = mock(LabelManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        FileSystemObjectManager manager = createManager(fileRepository, metaDataRepository, directoryRepository,
                ignoreFileRepository, associatedFileDataManager, importFileRepository, modelMapper, labelManager, fileSystem);

        Optional<FileSystemObject> result = manager.findFileSystemObject(new FileSystemObjectId(1, FileSystemObjectType.FSO_SOURCE));
        assertFalse(result.isPresent());
    }

    @Test
    void fsoFindByName() {
        ModelMapper modelMapper = mock(ModelMapper.class);
        FileRepository fileRepository = mock(FileRepository.class);
        MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        LabelManager labelManager = mock(LabelManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        FileSystemObjectManager manager = createManager(fileRepository, metaDataRepository, directoryRepository,
                ignoreFileRepository, associatedFileDataManager, importFileRepository, modelMapper, labelManager, fileSystem);

        AtomicInteger count = new AtomicInteger(0);
        manager.findFileSystemObjectByName("Test", FileSystemObjectType.FSO_SOURCE).forEach(nextDirectory -> {
            int i = count.get();
            count.set(++i);
        });
        assertEquals(0, count.get());
    }

    @Test
    void fsoPaths() {
        ModelMapper modelMapper = mock(ModelMapper.class);
        FileRepository fileRepository = mock(FileRepository.class);
        MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);
        LabelManager labelManager = mock(LabelManager.class);
        FileSystem fileSystem = mock(FileSystem.class);

        FileSystemObjectManager manager = createManager(fileRepository, metaDataRepository, directoryRepository,
                ignoreFileRepository, associatedFileDataManager, importFileRepository, modelMapper, labelManager, fileSystem);

        FileSystemObject testFso = mock(FileSystemObject.class);
        when(testFso.getParentId()).thenReturn(Optional.of(new FileSystemObjectId(1,FileSystemObjectType.FSO_DIRECTORY)));

        File result = manager.getFile(testFso);
        assertEquals("null", result.getName());
    }
}
