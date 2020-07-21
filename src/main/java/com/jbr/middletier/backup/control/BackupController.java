package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.dto.BackupDTO;
import com.jbr.middletier.backup.exception.ActionNotFoundException;
import com.jbr.middletier.backup.exception.BackupAlreadyExistsException;
import com.jbr.middletier.backup.exception.InvalidBackupIdException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Created by jason on 08/02/17.
 */

@RestController
@RequestMapping("/jbr/ext/backup")
public class BackupController {
    private static final Logger LOG = LoggerFactory.getLogger(BackupController.class);


    final private BackupRepository backupRepository;

    @Contract(pure = true)
    @Autowired
    BackupController(BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    @GetMapping(path="/byId")
    public @ResponseBody Backup specificBackup(@RequestParam(value="id", defaultValue="") String id) throws InvalidBackupIdException {
        LOG.info("List hardware.");
        // Check that the item exists.
        Optional<Backup> storedHardware = backupRepository.findById(id);

        if(!storedHardware.isPresent()) {
            throw new InvalidBackupIdException(id);
        }

        return storedHardware.get();
    }

    @GetMapping()
    public @ResponseBody Iterable<Backup> backups() {
        LOG.info("List backups Backup.");
        return backupRepository.findAll();
    }

    @PutMapping()
    public @ResponseBody OkStatus update(@NotNull @RequestBody BackupDTO backup) throws InvalidBackupIdException {
        LOG.info("Update backup - " + backup.getId());

        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());

        if(!storedBackup.isPresent()) {
            throw new InvalidBackupIdException(backup.getId());
        }

        storedBackup.get().update(backup);

        backupRepository.save(storedBackup.get());

        return OkStatus.getOkStatus();
    }

    @PostMapping()
    public @ResponseBody OkStatus create(@NotNull @RequestBody BackupDTO backup) throws BackupAlreadyExistsException {
        LOG.info("Create backup - " + backup.getId());

        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());
        if(storedBackup.isPresent()) {
            throw new BackupAlreadyExistsException(backup.getId());
        }

        backupRepository.save(new Backup(backup));

        return OkStatus.getOkStatus();
    }

    @DeleteMapping()
    public @ResponseBody OkStatus delete(@NotNull @RequestBody BackupDTO backup) throws InvalidBackupIdException {
        LOG.info("Delete backup - " + backup.getId());

        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());

        if(!storedBackup.isPresent()) {
            throw new InvalidBackupIdException(backup.getId());
        }

        backupRepository.delete(storedBackup.get());

        return OkStatus.getOkStatus();
    }
}
