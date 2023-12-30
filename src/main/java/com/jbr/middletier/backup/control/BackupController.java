package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.dto.BackupDTO;
import com.jbr.middletier.backup.exception.BackupAlreadyExistsException;
import com.jbr.middletier.backup.exception.InvalidBackupIdException;
import com.jbr.middletier.backup.schedule.BackupCtrl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.jbr.middletier.backup.util.CleanStringForLog.cleanString;

/**
 * Created by jason on 08/02/17.
 */

@RestController
@RequestMapping("/jbr/ext/backup")
public class BackupController {
    private static final Logger LOG = LoggerFactory.getLogger(BackupController.class);

    private final BackupRepository backupRepository;
    private final BackupCtrl backupCtrl;
    private final ModelMapper modelMapper;

    @Contract(pure = true)
    @Autowired
    BackupController(BackupRepository backupRepository,
                     BackupCtrl backupCtrl,
                     ModelMapper modelMapper) {
        this.backupRepository = backupRepository;
        this.backupCtrl = backupCtrl;
        this.modelMapper = modelMapper;
    }

    @GetMapping(path="/byId")
    public BackupDTO specificBackup(@RequestParam(value="id", defaultValue="") String id) throws InvalidBackupIdException {
        LOG.info("List hardware.");
        // Check that the item exists.
        Optional<Backup> storedHardware = backupRepository.findById(id);

        if(storedHardware.isEmpty()) {
            throw new InvalidBackupIdException(id);
        }

        return modelMapper.map(storedHardware.get(),BackupDTO.class);
    }

    @GetMapping()
    public Iterable<Backup> backups() {
        LOG.info("List backups Backup.");
        return backupRepository.findAllByOrderByIdAsc();
    }

    @PutMapping()
    public OkStatus update(@NotNull @RequestBody BackupDTO backup) throws InvalidBackupIdException {
        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());

        if(storedBackup.isEmpty()) {
            LOG.warn("Invalid backup id");
            throw new InvalidBackupIdException(backup.getId());
        }

        LOG.info("Update backup - {}", storedBackup.get().getId());
        backupRepository.save(modelMapper.map(backup, Backup.class));
        return OkStatus.getOkStatus();
    }

    @PostMapping()
    public OkStatus create(@NotNull @RequestBody BackupDTO backup) throws BackupAlreadyExistsException {
        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());
        if(storedBackup.isPresent()) {
            LOG.info("Backup already exist {}",storedBackup.get().getId());
            throw new BackupAlreadyExistsException(backup.getId());
        }

        Backup newBackup = modelMapper.map(backup,Backup.class);
        LOG.info("Create backup - {}", cleanString(newBackup.getId()));
        backupRepository.save(newBackup);

        return OkStatus.getOkStatus();
    }

    @PostMapping(path="/run")
    public OkStatus performBackup(@RequestParam(value="id", defaultValue="") String id) throws InvalidBackupIdException {
        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(id);
        if(storedBackup.isEmpty()) {
            LOG.warn("Invalid backup");
            throw new InvalidBackupIdException(id);
        }

        LOG.info("Perform the backup {}", storedBackup.get().getId());
        this.backupCtrl.performBackup(storedBackup.get());

        return OkStatus.getOkStatus();
    }

    @DeleteMapping()
    public OkStatus delete(@NotNull @RequestBody BackupDTO backup) throws InvalidBackupIdException {
            // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());

        if(storedBackup.isEmpty()) {
            LOG.info("Invalid backup id");
            throw new InvalidBackupIdException(backup.getId());
        }

        LOG.info("Delete the backup {}", storedBackup.get().getId());
        backupRepository.delete(storedBackup.get());

        return OkStatus.getOkStatus();
    }
}
