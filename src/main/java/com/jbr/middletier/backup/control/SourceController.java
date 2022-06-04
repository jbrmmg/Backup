package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Source;
import com.jbr.middletier.backup.dataaccess.SourceRepository;
import com.jbr.middletier.backup.dto.SourceDTO;
import com.jbr.middletier.backup.exception.InvalidSourceIdException;
import com.jbr.middletier.backup.exception.SourceAlreadyExistsException;
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
    private static final Logger LOG = LoggerFactory.getLogger(SourceController.class);

    private final SourceRepository sourceRepository;

    @Contract(pure = true)
    @Autowired
    public SourceController(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @GetMapping(path="/source")
    public @ResponseBody Iterable<Source> getSource() {
        LOG.info("Get the source");
        return sourceRepository.findAllByOrderByIdAsc();
    }

    @PostMapping(path="/source")
    public @ResponseBody Iterable<Source> createSource(@NotNull @RequestBody SourceDTO source) throws SourceAlreadyExistsException {
        if(source.getId() != null) {
            throw new SourceAlreadyExistsException(source.getId());
        }

        sourceRepository.save(new Source(source));

        return sourceRepository.findAllByOrderByIdAsc();
    }

    @PutMapping(path="/source")
    public @ResponseBody Iterable<Source> updateSource(@NotNull @RequestBody SourceDTO source) throws InvalidSourceIdException {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(!existing.isPresent()) {
            throw new InvalidSourceIdException(source.getId());
        }

        existing.get().update(source);
        sourceRepository.save(existing.get());

        return sourceRepository.findAllByOrderByIdAsc();
    }

    @DeleteMapping(path="/source")
    public @ResponseBody Iterable<Source> deleteSource(@RequestBody SourceDTO source) throws InvalidSourceIdException {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(!existing.isPresent()) {
            throw new InvalidSourceIdException(source.getId());
        }

        sourceRepository.deleteById(source.getId());

        return sourceRepository.findAllByOrderByIdAsc();
    }
}
