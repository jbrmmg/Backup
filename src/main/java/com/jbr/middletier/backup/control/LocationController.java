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

import java.util.ArrayList;
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

    private List<LocationDTO> getLocations() {
        List<LocationDTO> result = new ArrayList<>();

        associatedFileDataManager.findAllLocation().forEach(location -> result.add(associatedFileDataManager.convertToDTO(location)));
        LOG.info("Get the locations - {}", result.size());

        return result;
    }

    @GetMapping(path="/location")
    public List<LocationDTO> getLocation() {
        return getLocations();
    }

    @PostMapping(path="/location")
    public List<LocationDTO> createLocation(@NotNull @RequestBody LocationDTO location) throws LocationAlreadyExistsException {
        LOG.info("create location {}", location);
        associatedFileDataManager.createLocation(associatedFileDataManager.convertToEntity(location));
        return getLocations();
    }

    @PutMapping(path="/location")
    public List<LocationDTO> updateLocation(@NotNull @RequestBody LocationDTO location) throws InvalidLocationIdException {
        LOG.info("update location {}", location);
        associatedFileDataManager.updateLocation(associatedFileDataManager.convertToEntity(location));
        return getLocations();
    }

    @DeleteMapping(path="/location")
    public List<LocationDTO> deleteLocation(@NotNull @RequestBody LocationDTO location) throws InvalidLocationIdException {
        LOG.info("delete location {}", location);
        associatedFileDataManager.deleteLocation(associatedFileDataManager.convertToEntity(location));
        return getLocations();
    }

}
