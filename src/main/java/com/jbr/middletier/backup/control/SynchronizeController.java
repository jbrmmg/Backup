package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.data.Synchronize;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
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

    private final SourceRepository sourceRepository;

    @Contract(pure = true)
    @Autowired
    public SynchronizeController(SynchronizeRepository synchronizeRepository, SourceRepository sourceRepository) {
        this.synchronizeRepository = synchronizeRepository;
        this.sourceRepository = sourceRepository;
    }

    @GetMapping(path="/synchronize")
    public @ResponseBody Iterable<Synchronize> getSynchronize() {
        LOG.info("Get the synchronize");
        return synchronizeRepository.findAllByOrderByIdAsc();
    }

    @PostMapping(path="/synchronize")
    public @ResponseBody Iterable<Synchronize> createSynchronize(@NotNull @RequestBody SynchronizeDTO synchronize) throws SynchronizeAlreadyExistsException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(existing.isPresent()) {
            throw new SynchronizeAlreadyExistsException(existing.get().getId());
        }

        // Create a new Synchronize object.
        Synchronize newSync = new Synchronize(synchronize);

        // The source and destination need to be existing.
        Optional<Source> source = sourceRepository.findById(synchronize.getSource().getId());
        Optional<Source> destination = sourceRepository.findById(synchronize.getDestination().getId());

        if(!source.isPresent() || !destination.isPresent()) {
            //TODO - fail properly.
            return synchronizeRepository.findAllByOrderByIdAsc();
        }

        newSync.setSource(source.get());
        newSync.setDestination(destination.get());
        synchronizeRepository.save(newSync);

        return synchronizeRepository.findAllByOrderByIdAsc();
    }

    @PutMapping(path="/synchronize")
    public @ResponseBody Iterable<Synchronize> updateSynchronize(@NotNull @RequestBody SynchronizeDTO synchronize) throws InvalidSynchronizeIdException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new InvalidSynchronizeIdException(synchronize.getId());
        }

        existing.get().update(synchronize);

        // The source and destination need to be existing.
        Optional<Source> source = sourceRepository.findById(synchronize.getSource().getId());
        Optional<Source> destination = sourceRepository.findById(synchronize.getDestination().getId());

        if(!source.isPresent() || !destination.isPresent()) {
            //TODO - fail properly.
            return synchronizeRepository.findAllByOrderByIdAsc();
        }

        existing.get().setSource(source.get());
        existing.get().setDestination(destination.get());

        synchronizeRepository.save(existing.get());

        return synchronizeRepository.findAllByOrderByIdAsc();
    }

    @DeleteMapping(path="/synchronize")
    public @ResponseBody Iterable<Synchronize> deleteSynchronize(@RequestBody SynchronizeDTO synchronize) throws InvalidSynchronizeIdException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new InvalidSynchronizeIdException(synchronize.getId());
        }

        synchronizeRepository.deleteById(synchronize.getId());

        return synchronizeRepository.findAllByOrderByIdAsc();
    }
}
