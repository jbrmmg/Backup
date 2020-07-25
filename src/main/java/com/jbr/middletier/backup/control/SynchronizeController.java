package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Synchronize;
import com.jbr.middletier.backup.dataaccess.SynchronizeRepository;
import com.jbr.middletier.backup.dto.SynchronizeDTO;
import com.jbr.middletier.backup.exception.InvalidSynchronizeIdException;
import com.jbr.middletier.backup.exception.SynchronizeAlreadyExistsException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/jbr/ext/backup")
public class SynchronizeController {
    private static final Logger LOG = LoggerFactory.getLogger(SynchronizeController.class);

    private final SynchronizeRepository synchronizeRepository;

    @Contract(pure = true)
    @Autowired
    public SynchronizeController(SynchronizeRepository synchronizeRepository) {
        this.synchronizeRepository = synchronizeRepository;
    }

    @GetMapping(path="/synchronize")
    public @ResponseBody Iterable<Synchronize> getSynchronize() {
        LOG.info("Get the synchronize");
        return synchronizeRepository.findAll();
    }

    @PostMapping(path="/synchronize")
    public @ResponseBody Iterable<Synchronize> createSynchronize(@NotNull @RequestBody SynchronizeDTO synchronize) throws SynchronizeAlreadyExistsException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(existing.isPresent()) {
            throw new SynchronizeAlreadyExistsException(existing.get().getId());
        }

        synchronizeRepository.save(new Synchronize(synchronize));

        return synchronizeRepository.findAll();
    }

    @PutMapping(path="/synchronize")
    public @ResponseBody Iterable<Synchronize> updateSynchronize(@NotNull @RequestBody SynchronizeDTO synchronize) throws InvalidSynchronizeIdException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new InvalidSynchronizeIdException(synchronize.getId());
        }

        existing.get().update(synchronize);
        synchronizeRepository.save(existing.get());

        return synchronizeRepository.findAll();
    }

    @DeleteMapping(path="/synchronize")
    public @ResponseBody Iterable<Synchronize> deleteSynchronize(@RequestBody SynchronizeDTO synchronize) throws InvalidSynchronizeIdException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new InvalidSynchronizeIdException(synchronize.getId());
        }

        synchronizeRepository.deleteById(synchronize.getId());

        return synchronizeRepository.findAll();
    }
}
