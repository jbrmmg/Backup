package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.exception.InvalidLocationIdException;
import com.jbr.middletier.backup.exception.LocationAlreadyExistsException;
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
public class LocationController {
    private static final Logger LOG = LoggerFactory.getLogger(LocationController.class);

    private final AssociatedFileDataManager associatedFileDataManager;

    @Contract(pure = true)
    @Autowired
    public LocationController(AssociatedFileDataManager associatedFileDataManager) {
        this.associatedFileDataManager = associatedFileDataManager;
    }

    @GetMapping(path="/location")
    public @ResponseBody List<LocationDTO> getLocation() {
        LOG.info("Get the locations");
        return associatedFileDataManager.externalFindAllLocation();
    }

    @PostMapping(path="/location")
    public @ResponseBody List<LocationDTO> createLocation(@NotNull @RequestBody LocationDTO location) throws LocationAlreadyExistsException {
        associatedFileDataManager.createLocation(location);
        return associatedFileDataManager.externalFindAllLocation();
    }

    @PutMapping(path="/location")
    public @ResponseBody List<LocationDTO> updateLocation(@NotNull @RequestBody LocationDTO location) throws InvalidLocationIdException {
        associatedFileDataManager.updateLocation(location);
        return associatedFileDataManager.externalFindAllLocation();
    }

    @DeleteMapping(path="/location")
    public @ResponseBody List<LocationDTO> deleteLocation(@NotNull @RequestBody LocationDTO location) throws InvalidLocationIdException {
        associatedFileDataManager.deleteLocation(location);
        return associatedFileDataManager.externalFindAllLocation();
    }

}
