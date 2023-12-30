package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.exception.ClassificationIdException;
import com.jbr.middletier.backup.exception.InvalidClassificationIdException;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/jbr/ext/backup")
public class ClassificationController {
    private static final Logger LOG = LoggerFactory.getLogger(ClassificationController.class);

    private final AssociatedFileDataManager associatedFileDataManager;

    @Contract(pure = true)
    @Autowired
    public ClassificationController(AssociatedFileDataManager associatedFileDataManager) {
        this.associatedFileDataManager = associatedFileDataManager;
    }

    private List<ClassificationDTO> getLocations() {
        List<ClassificationDTO> result = new ArrayList<>();

        associatedFileDataManager.findAllClassifications().forEach(classification -> result.add(associatedFileDataManager.convertToDTO(classification)));
        LOG.info("Get the classifications - {}", result.size());

        return result;
    }

    @GetMapping(path="/classification")
    public List<ClassificationDTO> getClassification() {
        return getLocations();
    }

    @PostMapping(path="/classification")
    public List<ClassificationDTO> createClassification(@NotNull @RequestBody ClassificationDTO classification) throws ClassificationIdException {
        LOG.info("create classification {}", classification);
        associatedFileDataManager.createClassification(associatedFileDataManager.convertToEntity(classification));
        return getLocations();
    }

    @PutMapping(path="/classification")
    public List<ClassificationDTO> updateClassification(@NotNull @RequestBody ClassificationDTO classification) throws InvalidClassificationIdException {
        LOG.info("update classification {}", classification);
        associatedFileDataManager.updateClassification(associatedFileDataManager.convertToEntity(classification));
        return getLocations();
    }

    @DeleteMapping(path="/classification")
    public List<ClassificationDTO> deleteClassification(@NotNull @RequestBody ClassificationDTO classification) throws InvalidClassificationIdException {
        LOG.info("delete classification {}", classification);
        associatedFileDataManager.deleteClassification(associatedFileDataManager.convertToEntity(classification));
        return getLocations();
    }
}
