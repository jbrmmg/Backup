package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

@Controller
@RequestMapping("/jbr/int/backup")
public class DriveController {
    final static private Logger LOG = LoggerFactory.getLogger(DriveController.class);

    final private DirectoryRepository directoryRepository;
    final private FileRepository fileRepository;
    final private SourceRepository sourceRepository;
    final private ClassificationRepository classificationRepository;
    final private SynchronizeRepository synchronizeRepository;

    @Autowired
    public DriveController(DirectoryRepository directoryRepository,
                           FileRepository fileRepository,
                           SourceRepository sourceRepository,
                           ClassificationRepository classificationRepository,
                           SynchronizeRepository synchronizeRepository ) {
        this.directoryRepository = directoryRepository;
        this.fileRepository = fileRepository;
        this.sourceRepository = sourceRepository;
        this.classificationRepository = classificationRepository;
        this.synchronizeRepository = synchronizeRepository;
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

    @RequestMapping(path="/gather", method= RequestMethod.POST)
    public @ResponseBody OkStatus gather(@RequestBody String temp) {
        LOG.info("Process drive - " + temp);

        Iterable<Source> sources = sourceRepository.findAll();

        Iterable<Classification> classifications = classificationRepository.findAll();

        fileRepository.markAllRemoved();
        directoryRepository.markAllRemoved();

        for(Source nextSource: sources) {
            // Read directory structure into the database.
            try (Stream<Path> paths = Files.walk(Paths.get(nextSource.getPath()))) {
                paths
                        .filter(Files::isRegularFile)
                        .forEach(path -> processPath(path,nextSource,classifications));
            } catch (IOException e) {
                OkStatus result = new OkStatus();
                result.setStatus("FAILED");
                return result;
            }
        }

        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/sync", method= RequestMethod.POST)
    public @ResponseBody OkStatus synchronize(@RequestBody String temp) {
        LOG.info("Syncronize drives - " + temp);

        Iterable<Synchronize> synchronizes = synchronizeRepository.findAll();

        for(Synchronize nextSynchronize : synchronizes) {
            LOG.info(nextSynchronize.getSource().getPath() + " -> " + nextSynchronize.getDestination().getPath());
        }

        return OkStatus.getOkStatus();
    }
}
