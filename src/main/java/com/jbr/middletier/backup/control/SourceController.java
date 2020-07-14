package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/jbr/ext/backup")
public class SourceController {
    final static private Logger LOG = LoggerFactory.getLogger(ActionController.class);

    final private SourceRepository sourceRepository;

    @Contract(pure = true)
    @Autowired
    public SourceController(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @RequestMapping(path="/source", method= RequestMethod.GET)
    public @ResponseBody Iterable<Source> getSource() {
        LOG.info("Get the source");
        return sourceRepository.findAll();
    }

    @RequestMapping(path="/source", method=RequestMethod.POST)
    public @ResponseBody Iterable<Source> createSource(@NotNull @RequestBody Source source) throws Exception {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(existing.isPresent()) {
            throw new Exception(existing.get().getId() + " already exists");
        }

        sourceRepository.save(source);

        return sourceRepository.findAll();
    }

    @RequestMapping(path="/source", method=RequestMethod.PUT)
    public @ResponseBody Iterable<Source> updateSource(@NotNull @RequestBody Source source) throws Exception {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(!existing.isPresent()) {
            throw new Exception(source.getId() + " does not exist");
        }

        sourceRepository.save(source);

        return sourceRepository.findAll();
    }

    @RequestMapping(path="/source", method=RequestMethod.DELETE)
    public @ResponseBody Iterable<Source> deleteSource(@RequestBody Source source) throws Exception {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(!existing.isPresent()) {
            throw new Exception(source.getId() + " does not exist");
        }

        sourceRepository.delete(source);

        return sourceRepository.findAll();
    }
}
