package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dataaccess.*;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jbr/int/backup")
public class TestHelpController {
    private static final Logger LOG = LoggerFactory.getLogger(TestHelpController.class);

    private final FileRepository fileRepository;
    private final DirectoryRepository directoryRepository;
    private final IgnoreFileRepository ignoreFileRepository;
    private final ImportFileRepository importFileRepository;
    private final ActionConfirmRepository actionConfirmRepository;

    @Contract(pure = true)
    @Autowired
    public TestHelpController(FileRepository fileRepository, DirectoryRepository directoryRepository, IgnoreFileRepository ignoreFileRepository, ImportFileRepository importFileRepository, ActionConfirmRepository actionConfirmRepository) {
        this.fileRepository = fileRepository;
        this.directoryRepository = directoryRepository;
        this.ignoreFileRepository = ignoreFileRepository;
        this.importFileRepository = importFileRepository;
        this.actionConfirmRepository = actionConfirmRepository;
    }

    @DeleteMapping(path="/reset")
    public @ResponseBody
    String resetFileData() {
        LOG.info("Reset all file data in the database.");

        try {
            // Delete from actions
            actionConfirmRepository.deleteAll();

            // Delete from ignore files
            ignoreFileRepository.deleteAll();

            // Delete from import
            importFileRepository.deleteAll();

            // Delete from files
            fileRepository.deleteAll();

            // Delete from directory.
            directoryRepository.deleteAll();
        } catch(Exception ex) {
            LOG.error("Failed to reset", ex);
            return "FAIL";
        }

        return "OK";
    }
}
