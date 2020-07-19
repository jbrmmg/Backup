package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Synchronize;
import com.jbr.middletier.backup.dataaccess.SynchronizeRepository;
import com.jbr.middletier.backup.dto.SynchronizeDTO;
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
    final static private Logger LOG = LoggerFactory.getLogger(ActionController.class);

    final private SynchronizeRepository synchronizeRepository;

    @Contract(pure = true)
    @Autowired
    public SynchronizeController(SynchronizeRepository synchronizeRepository) {
        this.synchronizeRepository = synchronizeRepository;
    }

    @RequestMapping(path="/synchronize", method= RequestMethod.GET)
    public @ResponseBody Iterable<Synchronize> getSynchronize() {
        LOG.info("Get the synchronize");
        return synchronizeRepository.findAll();
    }

    @RequestMapping(path="/synchronize", method=RequestMethod.POST)
    public @ResponseBody Iterable<Synchronize> createSynchronize(@NotNull @RequestBody SynchronizeDTO synchronize) throws Exception {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(existing.isPresent()) {
            throw new Exception(existing.get().getId() + " already exists");
        }

        synchronizeRepository.save(new Synchronize(synchronize));

        return synchronizeRepository.findAll();
    }

    @RequestMapping(path="/synchronize", method=RequestMethod.PUT)
    public @ResponseBody Iterable<Synchronize> updateSynchronize(@NotNull @RequestBody SynchronizeDTO synchronize) throws Exception {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new Exception(synchronize.getId() + " does not exist");
        }

        existing.get().update(synchronize);
        synchronizeRepository.save(existing.get());

        return synchronizeRepository.findAll();
    }

    @RequestMapping(path="/synchronize", method=RequestMethod.DELETE)
    public @ResponseBody Iterable<Synchronize> deleteSynchronize(@RequestBody SynchronizeDTO synchronize) throws Exception {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new Exception(synchronize.getId() + " does not exist");
        }

        synchronizeRepository.deleteById(synchronize.getId());

        return synchronizeRepository.findAll();
    }
}
