package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class DriveManager {
    final static private Logger LOG = LoggerFactory.getLogger(DriveManager.class);

    final private DirectoryRepository directoryRepository;
    final private FileRepository fileRepository;
    final private SourceRepository sourceRepository;
    final private ClassificationRepository classificationRepository;
    final private SynchronizeRepository synchronizeRepository;
    final private BackupManager backupManager;
    final private ApplicationProperties applicationProperties;

    @Autowired
    public DriveManager(DirectoryRepository directoryRepository,
                        FileRepository fileRepository,
                        SourceRepository sourceRepository,
                        ClassificationRepository classificationRepository,
                        SynchronizeRepository synchronizeRepository,
                        BackupManager backupManager,
                        ApplicationProperties applicationProperties ) {
        this.directoryRepository = directoryRepository;
        this.fileRepository = fileRepository;
        this.sourceRepository = sourceRepository;
        this.classificationRepository = classificationRepository;
        this.synchronizeRepository = synchronizeRepository;
        this.backupManager = backupManager;
        this.applicationProperties = applicationProperties;
    }

    private Classification classifyFile(FileInfo file, Iterable<Classification> classifications)  {
        for(Classification nextClassification : classifications) {
            if(nextClassification.fileMatches(file)) {
                return nextClassification;
            }
        }

        return null;
    }

    private void processPath(Path path, Source nextSource, Iterable<Classification> classifications) {
        // Get the directory.
        Optional<DirectoryInfo> directory = directoryRepository.findBySourceAndPath(nextSource,
                path.toAbsolutePath().getParent().toString().replace(nextSource.getPath(),""));

        if(!directory.isPresent()) {
            DirectoryInfo newDirectoryInfo = new DirectoryInfo();
            newDirectoryInfo.setPath(path.toAbsolutePath().getParent().toString().replace(nextSource.getPath(),""));
            newDirectoryInfo.setSource(nextSource);

            directory = Optional.of(directoryRepository.save(newDirectoryInfo));
        } else {
            directory.get().clearRemoved();
            directoryRepository.save(directory.get());
        }


        // Does the file exist?
        Optional<FileInfo> file = fileRepository.findByDirectoryInfoAndName(directory.get(),path.getFileName().toString());

        if(!file.isPresent()) {
            // Get the file
            FileInfo newFile = new FileInfo();
            newFile.setName(path.getFileName().toString());
            newFile.setDirectoryInfo(directory.get());
            newFile.setClassification(classifyFile(newFile,classifications));
            newFile.setDate(new Date(path.toFile().lastModified()));
            newFile.setSize(path.toFile().length());
            newFile.clearRemoved();

            fileRepository.save(newFile);
        } else {
            if(file.get().getClassification() == null) {
                Classification newClassification = classifyFile(file.get(),classifications);

                if(newClassification != null) {
                    file.get().setClassification(newClassification);
                }
            }

            file.get().clearRemoved();
            fileRepository.save(file.get());
        }

        LOG.info(path.toString());
    }

    @Scheduled(cron = "#{@applicationProperties.gatherSchedule}")
    public void gatherCron() {
        if(applicationProperties.getSynchronizeEnabled()) {
            try {
                gather();
            } catch (Exception ex) {
                LOG.error("Failed to gather",ex);
            }
        }
    }

    public void gather() throws IOException {
        Iterable<Source> sources = sourceRepository.findAll();

        Iterable<Classification> classifications = classificationRepository.findAll();

        fileRepository.markAllRemoved();
        directoryRepository.markAllRemoved();

        for(Source nextSource: sources) {
            backupManager.postWebLog(BackupManager.webLogLevel.INFO, "Gather - " + nextSource.getPath());

            // Read directory structure into the database.
            try (Stream<Path> paths = Files.walk(Paths.get(nextSource.getPath()))) {
                paths
                        .filter(Files::isRegularFile)
                        .forEach(path -> processPath(path,nextSource,classifications));
            } catch (IOException e) {
                backupManager.postWebLog(BackupManager.webLogLevel.ERROR,"Failed to gather + " + e.toString());
                throw e;
            }
        }

        fileRepository.deleteRemoved();
        directoryRepository.deleteRemoved();
    }

    @Scheduled(cron = "#{@applicationProperties.synchronizeSchedule}")
    public void synchronizeCron()  {
        if(applicationProperties.getSynchronizeEnabled()) {
            try {
                synchronize();
            } catch (Exception ex) {
                LOG.error("Failed to syncrhonize",ex);
            }
        }
    }

    private void backup(SynchronizeStatus status) {
        LOG.info("Process backup - " + status.path + "//" + status.name);

    }

    private void warn(SynchronizeStatus status) {
        LOG.warn("File warning - " + status.path + "//" + status.name);
        backupManager.postWebLog(BackupManager.webLogLevel.WARN,"File warning - " + status.path + "/" + status.name);
    }

    private void delete(SynchronizeStatus status) {
        LOG.info("File should be deleted - " + status.path + "\\" + status.name);
        backupManager.postWebLog(BackupManager.webLogLevel.INFO,"File should be deleted - " + status.path + "/" + status.name);
    }

    public void synchronize() {
        Iterable<Synchronize> synchronizes = synchronizeRepository.findAll();

        for(Synchronize nextSynchronize : synchronizes) {
            backupManager.postWebLog(BackupManager.webLogLevel.INFO,"Synchronize - " + nextSynchronize.getSource().getPath() + " -> " + nextSynchronize.getDestination().getPath());

            for(SynchronizeStatus nextStatus: fileRepository.findSynchronizeStatus(nextSynchronize.getId())) {
                // Perform the appropriate actions
                switch(nextStatus.action) {
                    case "BACKUP":
                        backup(nextStatus);
                        break;

                    case "IGNORE":
                        break;

                    case "DELETE":
                        delete(nextStatus);
                        break;

                    case "WARN":
                        warn(nextStatus);
                        break;

                    default:
                        LOG.warn("Unexpected action - " + nextStatus.action + " " + nextStatus.path + " " + nextStatus.name);
                        backupManager.postWebLog(BackupManager.webLogLevel.WARN,"Unexpected action - " + nextStatus.action);
                }
            }

            LOG.info(nextSynchronize.getSource().getPath() + " -> " + nextSynchronize.getDestination().getPath());
        }
    }
}
