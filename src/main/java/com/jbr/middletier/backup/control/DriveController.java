package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.ClassificationRepository;
import com.jbr.middletier.backup.dataaccess.DirectoryRepository;
import com.jbr.middletier.backup.dataaccess.FileRepository;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
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
import java.util.Optional;
import java.util.stream.Stream;

@Controller
@RequestMapping("/jbr/int/backup/drive")
public class DriveController {
    final static private Logger LOG = LoggerFactory.getLogger(DriveController.class);

    final private DirectoryRepository directoryRepository;
    final private FileRepository fileRepository;
    final private SourceRepository sourceRepository;
    final private ClassificationRepository classificationRepository;

    @Autowired
    public DriveController(DirectoryRepository directoryRepository,
                           FileRepository fileRepository,
                           SourceRepository sourceRepository,
                           ClassificationRepository classificationRepository) {
        this.directoryRepository = directoryRepository;
        this.fileRepository = fileRepository;
        this.sourceRepository = sourceRepository;
        this.classificationRepository = classificationRepository;
    }

    private Classification classifyFile(File file, Iterable<Classification> classifications)  {
        for(Classification nextClassification : classifications) {
            if(nextClassification.fileMatches(file)) {
                return nextClassification;
            }
        }

        return null;
    }

    private void processPath(Path path, Source nextSource, Iterable<Classification> classifications) {
        // Get the directory.
        Optional<Directory> directory = directoryRepository.findBySourceAndPath(nextSource,
                path.toAbsolutePath().getParent().toString());

        if(!directory.isPresent()) {
            Directory newDirectory = new Directory();
            newDirectory.setPath(path.toAbsolutePath().getParent().toString());
            newDirectory.setSource(nextSource);

            directory = Optional.of(directoryRepository.save(newDirectory));
        }

        // Does the file exist?
        Optional<File> file = fileRepository.findByDirectoryAndName(directory.get(),path.getFileName().toString());

        if(!file.isPresent()) {
            // Get the file
            File newFile = new File();
            newFile.setName(path.getFileName().toString());
            newFile.setDirectory(directory.get());
            newFile.setClassification(classifyFile(newFile,classifications));

            fileRepository.save(newFile);
        } else {
            if(file.get().getClassification() == null) {
                Classification newClassification = classifyFile(file.get(),classifications);

                if(newClassification != null) {
                    file.get().setClassification(newClassification);

                    fileRepository.save(file.get());
                }
            }
        }

        LOG.info(path.toString());
    }

    @RequestMapping(method= RequestMethod.POST)
    public @ResponseBody OkStatus process(@RequestBody String temp) {
        LOG.info("Process drive - " + temp);

        Iterable<Source> sources = sourceRepository.findAll();

        Iterable<Classification> classifications = classificationRepository.findAll();

        for(Source nextSource: sources) {
            // Read directory structure into the database.
            try (Stream<Path> paths = Files.walk(Paths.get(nextSource.getPath()))) {
                paths
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            processPath(path,nextSource,classifications);
                        });
            } catch (IOException e) {
                OkStatus result = new OkStatus();
                result.setStatus("FAILED");
                return result;
            }
        }

        return OkStatus.getOkStatus();
    }
}
