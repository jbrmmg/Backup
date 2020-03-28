package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Backup;
import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.exception.BackupAlreadyExistsException;
import com.jbr.middletier.backup.exception.InvalidBackupIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Created by jason on 08/02/17.
 */

@Controller
@RequestMapping("/jbr/ext/backup")
public class BackupController {
    final static private Logger LOG = LoggerFactory.getLogger(BackupController.class);


    final private BackupRepository backupRepository;

    @Autowired
    BackupController(BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    @RequestMapping(path="/byId",method=RequestMethod.GET)
    public @ResponseBody Backup specificBackup(@RequestParam(value="id", defaultValue="") String id) throws InvalidBackupIdException {
        LOG.info("List hardware.");
        // Check that the item exists.
        Optional<Backup> storedHardware = backupRepository.findById(id);

        if(!storedHardware.isPresent()) {
            throw new InvalidBackupIdException(id);
        }

        return storedHardware.get();
    }

    @RequestMapping(method=RequestMethod.GET)
    public @ResponseBody Iterable<Backup> backups() {
        LOG.info("List backups Backup.");
        return backupRepository.findAll();
    }

    @RequestMapping(method=RequestMethod.PUT)
    public @ResponseBody OkStatus update(@RequestBody Backup backup) throws InvalidBackupIdException {
        LOG.info("Update backup - " + backup.getId());

        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());

        if(!storedBackup.isPresent()) {
            throw new InvalidBackupIdException(backup.getId());
        }

        storedBackup.get().setArtifact(backup.getArtifact());
        storedBackup.get().setBackupName(backup.getBackupName());
        storedBackup.get().setDirectory(backup.getDirectory());
        storedBackup.get().setTime(backup.getTime());
        storedBackup.get().setType(backup.getType());
        storedBackup.get().setFileName(backup.getFileName());

        backupRepository.save(storedBackup.get());

        return OkStatus.getOkStatus();
    }

    @RequestMapping(method=RequestMethod.POST)
    public @ResponseBody OkStatus create(@RequestBody Backup backup) throws BackupAlreadyExistsException {
        LOG.info("Create backup - " + backup.getId());

        // Check that the item exists.
        Optional<Backup> storedBackup = backupRepository.findById(backup.getId());
        if(storedBackup.isPresent()) {
            throw new BackupAlreadyExistsException(backup.getId());
        }

        backupRepository.save(backup);

        return OkStatus.getOkStatus();
    }

    @RequestMapping(method=RequestMethod.DELETE)
    public @ResponseBody OkStatus delete(@RequestBody Backup backup) throws InvalidBackupIdException {
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
