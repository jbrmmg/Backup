package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.ImportFileBaseDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.FileProcessor;
import com.jbr.middletier.backup.manager.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImportFileWorker implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFileWorker.class);

    private final ImportFileWorkQueue queue;
    private final ImportManager manager;
    private final FileSystem fileSystem;
    private final File preImportSource;
    private boolean running = false;

    public ImportFileWorker(ImportFileWorkQueue queue, ImportManager manager, FileSystem fileSystem) {
        this.queue = queue;
        this.manager = manager;
        this.fileSystem = fileSystem;

        Optional<PreImportSource> source = this.manager.findPreImportSource();
        preImportSource = source.map(importSource -> new File(importSource.getPath())).orElse(null);
    }

    private boolean getMD5(PreImportFileDTO file, File realWorldFile) {
        try {
            Classification dummyClassification = new Classification();
            dummyClassification.setUseMD5(true);
            MD5 md5 = fileSystem.getClassifiedFileMD5(realWorldFile.toPath(), dummyClassification, 0);
            file.setMd5(md5);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void readFileData(PreImportFileDTO file) {
        try {
            if(preImportSource == null) {
                // Cannot process without this.
                throw new IllegalStateException("Pre-Import Source is null");
            }

            // Read the file size and last modified date.
            File realWorldFile = new File(preImportSource.getPath(),file.getFilename());

            if(!realWorldFile.exists()){
                throw new IllegalStateException("File does not exist");
            }

            file.setSize(realWorldFile.length());
            file.setDate(FileProcessor.getFileLastModified(realWorldFile));

            boolean md5OK = getMD5(file, realWorldFile);

            file.setImmediateImported(md5OK ? TrafficLightType.TL_GREEN : TrafficLightType.TL_AMBER);
        } catch (Exception e) {
            // Do something here
            file.setImmediateImported(TrafficLightType.TL_RED);
        }
        file.update();
    }

    private void getImportData(PreImportFileDTO file) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        file.setImported(TrafficLightType.TL_AMBER);
        file.update();
    }

    private ImportFileBaseDTO getSimilar(FileInfo fileInfo) {
        ImportFileBaseDTO similar = new ImportFileBaseDTO();
        similar.setFilename(fileInfo.getName() + " [" + fileInfo.getIdAndType().getType().getTypeName() + "]");
        similar.setSize(fileInfo.getSize());
        similar.setMd5(fileInfo.getMD5());
        similar.setDate(fileInfo.getDate());

        // Get the full filename.
/*        File file = fileSystemObjectManager.getFile(fileInfo);
        if(!file.getPath().equalsIgnoreCase(file.getName())) {
            // Only accept file if its from the valid source.
            AtomicBoolean accept = new AtomicBoolean(false);
            validSources.forEach(source -> {
                if(file.getPath().contains(source.getPath())) {
                    accept.set(true);
                }
            });

            if(!accept.get()) {
                return null;
            }

            similar.setFilename(file.getPath());
        }*/

        return similar;
    }

    private void getIgnoredStatus(PreImportFileDTO file) {
        // Get details of ignored files that match on name and or MD5.
        List<FileInfo> similar = manager.getSimilarIgnore(file.getFilename(), file.getMd5());

        // Check for an exact match.
        for(FileInfo fileInfo : similar) {
            if(fileInfo.getName().equals(file.getFilename()) && fileInfo.getMD5().toString().equalsIgnoreCase(file.getMd5())) {
                // Add this to the list of similar files.
                file.addSimilarFile(getSimilar(fileInfo));
                file.setIgnored(TrafficLightType.TL_RED);
                file.update();
                return;
            }
        }

        if(!similar.isEmpty()) {
            for(FileInfo fileInfo : similar) {
                // Add to the similar list.
                file.addSimilarFile(getSimilar(fileInfo));
            }

            file.setIgnored(TrafficLightType.TL_AMBER);
            file.update();
            return;
        }

        // If we get here then the file is not ignored or similar to an ignored file.
        file.setIgnored(TrafficLightType.TL_GREEN);
        file.update();
    }

    private void getDuplicateStatus(PreImportFileDTO file) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        file.setDuplicated(TrafficLightType.TL_RED);
        file.update();
    }

    private void processFile(PreImportFileDTO file) {
        try {
            if(file == null) {
                return;
            }

            // Determine what to do on this file.
            if (file.getImmediateImported().equals(TrafficLightType.TL_UNKNOWN)) {
                readFileData(file);
            } else if (file.getIgnored().equals(TrafficLightType.TL_UNKNOWN)) {
                getIgnoredStatus(file);
            }else if (file.getImported().equals(TrafficLightType.TL_UNKNOWN)) {
                getImportData(file);
            }  else if (file.getDuplicated().equals(TrafficLightType.TL_UNKNOWN)) {
                getDuplicateStatus(file);
            }

            this.manager.queueForUpdates(file);
        } catch (Exception e) {
            LOG.warn(e.getMessage(),e);
        }
    }

    @Override
    public void run() {
        this.running = true;

        // Process instructions from the queue.
        while (running) {
            if(queue.isEmpty()) {
                try {
                    queue.waitIsNotEmpty();
                } catch (InterruptedException e) {
//                    log.severe("Error while waiting to Consume messages.");
                    break;
                }
            }
            if (!running) {
                break;
            }

            processFile(queue.poll());
        }
    }

    public void stop() {
        this.running = false;
    }
}
