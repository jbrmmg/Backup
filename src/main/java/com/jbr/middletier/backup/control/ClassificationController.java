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

    @GetMapping(path="/classification")
    public @ResponseBody List<ClassificationDTO> getClassification() {
        LOG.info("Get the classifications.");
        return associatedFileDataManager.externalFindAllClassification();
    }

    @PostMapping(path="/classification")
    public @ResponseBody List<ClassificationDTO> createClassification(@NotNull @RequestBody ClassificationDTO classification) throws ClassificationIdException {
        associatedFileDataManager.createClassification(classification);
        return associatedFileDataManager.externalFindAllClassification();
    }

    @PutMapping(path="/classification")
    public @ResponseBody List<ClassificationDTO> updateClassification(@NotNull @RequestBody ClassificationDTO classification) throws InvalidClassificationIdException {
        associatedFileDataManager.updateClassification(classification);
        return associatedFileDataManager.externalFindAllClassification();
    }

    @DeleteMapping(path="/classification")
    public @ResponseBody List<ClassificationDTO> deleteClassification(@NotNull @RequestBody ClassificationDTO classification) throws InvalidClassificationIdException {
        associatedFileDataManager.deleteClassification(classification);
        return associatedFileDataManager.externalFindAllClassification();
    }
}
