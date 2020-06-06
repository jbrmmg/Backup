package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Classification;
import com.jbr.middletier.backup.dataaccess.ClassificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/jbr/ext/backup")
public class ClassificationController {
    final static private Logger LOG = LoggerFactory.getLogger(ClassificationController.class);

    final private ClassificationRepository classificationRepository;

    @Autowired
    public ClassificationController(ClassificationRepository classificationRepository) {
        this.classificationRepository = classificationRepository;
    }

    @RequestMapping(path="/classification", method= RequestMethod.GET)
    public @ResponseBody Iterable<Classification> getClassification() {
        LOG.info("Get the classifications.");
        return classificationRepository.findAll();
    }

    @RequestMapping(path="/classification", method=RequestMethod.POST)
    public @ResponseBody Iterable<Classification> createClassification(@RequestBody Classification classification) throws Exception {
        Optional<Classification> existing = classificationRepository.findById(classification.getId());
        if(existing.isPresent()) {
            throw new Exception(existing.get().getId() + " already exists");
        }

        classificationRepository.save(classification);

        return classificationRepository.findAll();
    }

    @RequestMapping(path="/classification", method=RequestMethod.PUT)
    public @ResponseBody Iterable<Classification> updateClassification(@RequestBody Classification classification) throws Exception {
        Optional<Classification> existing = classificationRepository.findById(classification.getId());
        if(!existing.isPresent()) {
            throw new Exception(classification.getId() + " does not exist");
        }

        classificationRepository.save(classification);

        return classificationRepository.findAll();
    }

    @RequestMapping(path="/classification", method=RequestMethod.DELETE)
    public @ResponseBody Iterable<Classification> deleteClassification(@RequestBody Classification classification) throws Exception {
        Optional<Classification> existing = classificationRepository.findById(classification.getId());
        if(!existing.isPresent()) {
            throw new Exception(classification.getId() + " does not exist");
        }

        classificationRepository.delete(classification);

        return classificationRepository.findAll();
    }
}
