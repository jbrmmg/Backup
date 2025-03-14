package com.jbr.middletier.backup.manager.importing;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.ImportFileBaseDTO;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.FileProcessor;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.FileSystemImageData;
import com.jbr.middletier.backup.util.LatLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class ImportFileWorker implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFileWorker.class);

    private final ImportFileWorkQueue queue;
    private final ImportManager manager;
    private final FileSystem fileSystem;
    private final File preImportSource;

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

    private void readAdditionalDataFromFile(File realWorldFile, PreImportFileDTO file) {
        try {
            // Attempt to read the additional data from the file.
            Optional<FileSystemImageData> imageData = fileSystem.readImageMetaData(realWorldFile);

            if(imageData.isPresent()) {
                LatLong latLong = imageData.get().getLatLong();
                LOG.info("LAT/LONG = {} {}", latLong.getLatitude(), latLong.getLongitude());
            }

            LOG.info("Read image.");
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
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

            readAdditionalDataFromFile(realWorldFile, file);

            file.setImmediateImported(md5OK ? TrafficLightType.TL_GREEN : TrafficLightType.TL_AMBER);
        } catch (Exception e) {
            // Do something here
            file.setImmediateImported(TrafficLightType.TL_RED);
        }
        file.update();
    }

    private void getImportData(PreImportFileDTO file) {
        // Has this file been processed and imported?
        List<FileInfo> similar = manager.getImport(file.getFilename());

        // If there is an import file then add it as a similar file.
        for(FileInfo fileInfo : similar) {
            if(fileInfo.getName().equals(file.getFilename())) {
                // Add this to the list of similar files.
                file.addSimilarFile(getSimilar(fileInfo));
                file.setImported(TrafficLightType.TL_GREEN);
                file.update();
                return;
            }
        }

        file.setImported(TrafficLightType.TL_RED);
        file.update();
    }

    private ImportFileBaseDTO getSimilar(FileInfo fileInfo) {
        ImportFileBaseDTO similar = new ImportFileBaseDTO();
        similar.setFilename(fileInfo.getName() + " [" + fileInfo.getIdAndType().getType().getTypeName() + "]");
        similar.setSize(fileInfo.getSize());
        similar.setMd5(fileInfo.getMD5());
        similar.setDate(fileInfo.getDate());

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
        // Get details of files that already exist that match on name or MD5.
        List<ImportFileBaseDTO> similar = manager.getSimilarImported(file.getFilename(), file.getMd5());

        // If there are no similar files then, this file is yet to be imported.
        if(similar.isEmpty()) {
            file.setDuplicated(TrafficLightType.TL_AMBER);
            file.update();
            return;
        }

        // How many of these files have the same MD5?
        int matchMd5 = 0;
        for(ImportFileBaseDTO fileInfo : similar) {
            if(fileInfo.getMd5().equalsIgnoreCase(file.getMd5())) {
                matchMd5++;
            }
        }

        // If there is one file with the right MD5, and it matches on name and also md5, then this file has been imported successfully.
        if(matchMd5 == 1) {
            for(ImportFileBaseDTO fileInfo : similar) {
                if(fileInfo.getMd5().equalsIgnoreCase(file.getMd5()) && fileInfo.getFilename().toLowerCase().endsWith(file.getFilename().toLowerCase())) {
                    file.setDuplicated(TrafficLightType.TL_GREEN);
                    file.update();
                    file.addSimilarFile(fileInfo);
                    return;
                }
            }
        }

        // If there is a file that matches on MD5 then this is a potential duplicate.
        if(matchMd5 > 1) {
            file.setDuplicated(TrafficLightType.TL_RED);
            for(ImportFileBaseDTO fileInfo : similar) {
                if(fileInfo.getMd5().equalsIgnoreCase(file.getMd5())) {
                    // Add all the files that have the same MD5.
                    file.addSimilarFile(fileInfo);
                }
            }
            file.update();
            return;
        }

        // This means none of the existing files match on the MD5 but happen to have different names (so the file is still to be actually imported).
        file.setDuplicated(TrafficLightType.TL_AMBER);
        for(ImportFileBaseDTO fileInfo : similar) {
            // Add all the files that have the same MD5.
            file.addSimilarFile(fileInfo);
        }
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
        // Process instructions from the queue.
        while (true) {
            if(queue.isEmpty()) {
                try {
                    queue.waitIsNotEmpty();
                } catch (InterruptedException e) {
                    break;
                }
            }

            processFile(queue.poll());
        }
    }
}
