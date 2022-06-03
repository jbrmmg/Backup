package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Classification;
import com.jbr.middletier.backup.dataaccess.ClassificationRepository;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.exception.ClassificationIdException;
import com.jbr.middletier.backup.exception.InvalidClassificationIdException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/jbr/ext/backup")
public class ClassificationController {
    private static final Logger LOG = LoggerFactory.getLogger(ClassificationController.class);

    private final ClassificationRepository classificationRepository;

    @Contract(pure = true)
    @Autowired
    public ClassificationController(ClassificationRepository classificationRepository) {
        this.classificationRepository = classificationRepository;
    }

    @GetMapping(path="/classification")
    public @ResponseBody Iterable<Classification> getClassification() {
        LOG.info("Get the classifications.");
        return classificationRepository.findAllByOrderByIdAsc();
    }

    @PostMapping(path="/classification")
    public @ResponseBody Iterable<Classification> createClassification(@NotNull @RequestBody ClassificationDTO classification) throws ClassificationIdException {
        if(classification.getId() != null) {
            throw new ClassificationIdException();
        }

        classificationRepository.save(new Classification(classification));

        return classificationRepository.findAllByOrderByIdAsc();
    }

    @PutMapping(path="/classification")
    public @ResponseBody Iterable<Classification> updateClassification(@NotNull @RequestBody ClassificationDTO classification) throws InvalidClassificationIdException {
        Optional<Classification> existing = classificationRepository.findById(classification.getId());
        if(!existing.isPresent()) {
            throw new InvalidClassificationIdException(classification.getId());
        }

        existing.get().update(classification);

        classificationRepository.save(existing.get());

        return classificationRepository.findAllByOrderByIdAsc();
    }

    @DeleteMapping(path="/classification")
    public @ResponseBody Iterable<Classification> deleteClassification(@NotNull @RequestBody ClassificationDTO classification) throws InvalidClassificationIdException {
        Optional<Classification> existing = classificationRepository.findById(classification.getId());
        if(!existing.isPresent()) {
            throw new InvalidClassificationIdException(classification.getId());
        }

        classificationRepository.deleteById(classification.getId());

        return classificationRepository.findAllByOrderByIdAsc();
    }
}
