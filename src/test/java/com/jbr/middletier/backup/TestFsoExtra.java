package com.jbr.middletier.backup;

import com.jbr.middletier.backup.data.DirectoryInfo;
import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.IgnoreFileRepository;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestFsoExtra {
    @Test
    public void findByTypeDirectory() {
        FileRepository fileRepository = mock(FileRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);

        DirectoryInfo directory = new DirectoryInfo();
        directory.setName("Test");
        List<DirectoryInfo> testList = new ArrayList<>();
        testList.add(directory);
        when(directoryRepository.findAllByOrderByIdAsc()).thenReturn(testList);

        FileSystemObjectManager manager = new FileSystemObjectManager(fileRepository,
                directoryRepository,
                ignoreFileRepository,
                associatedFileDataManager,
                importFileRepository);

        AtomicInteger count = new AtomicInteger(0);
        manager.findAllByType(FileSystemObjectType.FSO_DIRECTORY).forEach(nextDirectory -> {
            Assert.assertEquals("Test", nextDirectory.getName());
            int i = count.get();
            count.set(++i);
        });
        Assert.assertEquals(1, count.get());
    }

    @Test
    public void findByTypeDirectory2() {
        FileRepository fileRepository = mock(FileRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);

        DirectoryInfo directory = new DirectoryInfo();
        directory.setName("Test");
        List<DirectoryInfo> testList = new ArrayList<>();
        testList.add(directory);
        when(directoryRepository.findAllByOrderByIdAsc()).thenReturn(testList);

        FileSystemObjectManager manager = new FileSystemObjectManager(fileRepository,
                directoryRepository,
                ignoreFileRepository,
                associatedFileDataManager,
                importFileRepository);

        AtomicInteger count = new AtomicInteger(0);
        manager.findAllByType(FileSystemObjectType.FSO_SOURCE).forEach(nextDirectory -> {
            int i = count.get();
            count.set(++i);
        });
        Assert.assertEquals(0, count.get());
    }

    @Test
    public void fsoSaveUnsupported() {
        FileRepository fileRepository = mock(FileRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);

        FileSystemObject fso = mock(FileSystemObject.class);
        when(fso.getIdAndType()).thenReturn(new FileSystemObjectId(0, FileSystemObjectType.FSO_SOURCE));

        FileSystemObjectManager manager = new FileSystemObjectManager(fileRepository,
                directoryRepository,
                ignoreFileRepository,
                associatedFileDataManager,
                importFileRepository);

        try {
            manager.save(fso);
            Assert.fail();
        } catch(IllegalStateException e) {
            Assert.assertEquals("Save not supported for 0", e.getMessage());
        }
    }

    @Test
    public void fsoDeleteUnsupported() {
        FileRepository fileRepository = mock(FileRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);

        FileSystemObject fso = mock(FileSystemObject.class);
        when(fso.getIdAndType()).thenReturn(new FileSystemObjectId(0, FileSystemObjectType.FSO_SOURCE));

        FileSystemObjectManager manager = new FileSystemObjectManager(fileRepository,
                directoryRepository,
                ignoreFileRepository,
                associatedFileDataManager,
                importFileRepository);

        try {
            manager.delete(fso);
            Assert.fail();
        } catch(IllegalStateException e) {
            Assert.assertEquals("Delete not supported for 0", e.getMessage());
        }
    }

    @Test
    public void fsoFind1() {
        FileRepository fileRepository = mock(FileRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);

        when(ignoreFileRepository.findById(1)).thenReturn(Optional.empty());

        FileSystemObjectManager manager = new FileSystemObjectManager(fileRepository,
                directoryRepository,
                ignoreFileRepository,
                associatedFileDataManager,
                importFileRepository);

        Optional<FileSystemObject> result = manager.findFileSystemObject(new FileSystemObjectId(1, FileSystemObjectType.FSO_IGNORE_FILE));
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void fsoFind2() {
        FileRepository fileRepository = mock(FileRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);

        when(importFileRepository.findById(1)).thenReturn(Optional.empty());

        FileSystemObjectManager manager = new FileSystemObjectManager(fileRepository,
                directoryRepository,
                ignoreFileRepository,
                associatedFileDataManager,
                importFileRepository);

        Optional<FileSystemObject> result = manager.findFileSystemObject(new FileSystemObjectId(1, FileSystemObjectType.FSO_IMPORT_FILE));
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void fsoFind3() {
        FileRepository fileRepository = mock(FileRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);

        FileSystemObjectManager manager = new FileSystemObjectManager(fileRepository,
                directoryRepository,
                ignoreFileRepository,
                associatedFileDataManager,
                importFileRepository);

        Optional<FileSystemObject> result = manager.findFileSystemObject(new FileSystemObjectId(1, FileSystemObjectType.FSO_SOURCE));
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void fsoFindByName() {
        FileRepository fileRepository = mock(FileRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);

        FileSystemObjectManager manager = new FileSystemObjectManager(fileRepository,
                directoryRepository,
                ignoreFileRepository,
                associatedFileDataManager,
                importFileRepository);

        AtomicInteger count = new AtomicInteger(0);
        manager.findFileSystemObjectByName("Test", FileSystemObjectType.FSO_SOURCE).forEach(nextDirectory -> {
            int i = count.get();
            count.set(++i);
        });
        Assert.assertEquals(0, count.get());
    }

    @Test
    public void fsoPaths() {
        FileRepository fileRepository = mock(FileRepository.class);
        DirectoryRepository directoryRepository = mock(DirectoryRepository.class);
        IgnoreFileRepository ignoreFileRepository = mock(IgnoreFileRepository.class);
        AssociatedFileDataManager associatedFileDataManager = mock(AssociatedFileDataManager.class);
        ImportFileRepository importFileRepository = mock(ImportFileRepository.class);

        FileSystemObjectManager manager = new FileSystemObjectManager(fileRepository,
                directoryRepository,
                ignoreFileRepository,
                associatedFileDataManager,
                importFileRepository);

        FileSystemObject testFso = mock(FileSystemObject.class);
        when(testFso.getParentId()).thenReturn(new FileSystemObjectId(1,FileSystemObjectType.FSO_DIRECTORY));

        File result = manager.getFile(testFso);
        Assert.assertEquals("null", result.getName());
    }
}
