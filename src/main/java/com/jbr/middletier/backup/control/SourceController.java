package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import com.jbr.middletier.backup.dto.SourceDTO;
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
    private static final Logger LOG = LoggerFactory.getLogger(ActionController.class);

    final private SourceRepository sourceRepository;

    @Contract(pure = true)
    @Autowired
    public SourceController(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @GetMapping(path="/source")
    public @ResponseBody Iterable<Source> getSource() {
        LOG.info("Get the source");
        return sourceRepository.findAll();
    }

    @PostMapping(path="/source")
    public @ResponseBody Iterable<Source> createSource(@NotNull @RequestBody SourceDTO source) throws Exception {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(existing.isPresent()) {
            throw new Exception(existing.get().getId() + " already exists");
        }

        sourceRepository.save(new Source(source));

        return sourceRepository.findAll();
    }

    @PutMapping(path="/source")
    public @ResponseBody Iterable<Source> updateSource(@NotNull @RequestBody SourceDTO source) throws Exception {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(!existing.isPresent()) {
            throw new Exception(source.getId() + " does not exist");
        }

        existing.get().update(source);
        sourceRepository.save(existing.get());

        return sourceRepository.findAll();
    }

    @DeleteMapping(path="/source")
    public @ResponseBody Iterable<Source> deleteSource(@RequestBody SourceDTO source) throws Exception {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(!existing.isPresent()) {
            throw new Exception(source.getId() + " does not exist");
        }

        sourceRepository.deleteById(source.getId());

        return sourceRepository.findAll();
    }
}
