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
    public @ResponseBody BackupDTO specificBackup(@RequestParam(value="id", defaultValue="") String id) throws InvalidBackupIdException {
        LOG.info("List hardware.");
        // Check that the item exists.
        Optional<Backup> storedHardware = backupRepository.findById(id);

        if(storedHardware.isEmpty()) {
            throw new InvalidBackupIdException(id);
        }

        return modelMapper.map(storedHardware.get(),BackupDTO.class);
    }

    @GetMapping()
    public @ResponseBody Iterable<Backup> backups() {
        LOG.info("List backups Backup.");
        return backupRepository.findAllByOrderByIdAsc();
    }

    @PutMapping()
    public @ResponseBody OkStatus update(@NotNull @RequestBody BackupDTO backup) throws InvalidBackupIdException {
        LOG.info("Update backup - {}", backup.getId());

        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());

        if(storedBackup.isEmpty()) {
            throw new InvalidBackupIdException(backup.getId());
        }

        backupRepository.save(modelMapper.map(backup, Backup.class));
        return OkStatus.getOkStatus();
    }

    @PostMapping()
    public @ResponseBody OkStatus create(@NotNull @RequestBody BackupDTO backup) throws BackupAlreadyExistsException {
        LOG.info("Create backup - {}", backup.getId());

        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());
        if(storedBackup.isPresent()) {
            throw new BackupAlreadyExistsException(backup.getId());
        }

        backupRepository.save(modelMapper.map(backup,Backup.class));

        return OkStatus.getOkStatus();
    }

    @PostMapping(path="/run")
    public @ResponseBody OkStatus performBackup(@RequestParam(value="id", defaultValue="") String id) throws InvalidBackupIdException {
        LOG.info("Perform backup - {}", id);

        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(id);
        if(storedBackup.isEmpty()) {
            throw new InvalidBackupIdException(id);
        }

        this.backupCtrl.performBackup(storedBackup.get());

        return OkStatus.getOkStatus();
    }

    @DeleteMapping()
    public @ResponseBody OkStatus delete(@NotNull @RequestBody BackupDTO backup) throws InvalidBackupIdException {
        LOG.info("Delete backup - {}", backup.getId());

        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());

        if(storedBackup.isEmpty()) {
            throw new InvalidBackupIdException(backup.getId());
        }

        backupRepository.delete(storedBackup.get());

        return OkStatus.getOkStatus();
    }
}
