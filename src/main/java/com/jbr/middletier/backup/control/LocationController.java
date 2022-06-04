package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Location;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.LocationDTO;
import com.jbr.middletier.backup.exception.InvalidLocationIdException;
import com.jbr.middletier.backup.exception.LocationAlreadyExistsException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/jbr/ext/backup")
public class LocationController {
    private static final Logger LOG = LoggerFactory.getLogger(LocationController.class);

    private final LocationRepository locationRepository;

    @Contract(pure = true)
    @Autowired
    public LocationController(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @GetMapping(path="/location")
    public @ResponseBody
    Iterable<Location> getLocation() {
        LOG.info("Get the locations");
        return locationRepository.findAllByOrderByIdAsc();
    }

    @PostMapping(path="/location")
    public @ResponseBody Iterable<Location> createLocation(@NotNull @RequestBody LocationDTO location) throws LocationAlreadyExistsException {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(existing.isPresent()) {
            throw new LocationAlreadyExistsException(existing.get().getId());
        }

        locationRepository.save(new Location(location));

        return locationRepository.findAllByOrderByIdAsc();
    }

    @PutMapping(path="/location")
    public @ResponseBody Iterable<Location> updateLocation(@NotNull @RequestBody LocationDTO location) throws InvalidLocationIdException {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(!existing.isPresent()) {
            throw new InvalidLocationIdException(location.getId());
        }

        existing.get().update(location);
        locationRepository.save(existing.get());

        return locationRepository.findAllByOrderByIdAsc();
    }

    @DeleteMapping(path="/location")
    public @ResponseBody Iterable<Location> deleteLocation(@NotNull @RequestBody LocationDTO location) throws InvalidLocationIdException {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(!existing.isPresent()) {
            throw new InvalidLocationIdException(location.getId());
        }

        locationRepository.deleteById(location.getId());

        return locationRepository.findAllByOrderByIdAsc();
    }

}
