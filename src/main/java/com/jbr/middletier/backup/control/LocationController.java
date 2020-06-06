package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Location;
import com.jbr.middletier.backup.dataaccess.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/jbr/ext/backup")
public class LocationController {
    final static private Logger LOG = LoggerFactory.getLogger(LocationController.class);

    final private LocationRepository locationRepository;

    @Autowired
    public LocationController(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @RequestMapping(path="/location", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Location> getLocation() {
        LOG.info("Get the locations");
        return locationRepository.findAll();
    }

    @RequestMapping(path="/location", method=RequestMethod.POST)
    public @ResponseBody Iterable<Location> createLocation(@RequestBody Location location) throws Exception {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(existing.isPresent()) {
            throw new Exception(existing.get().getId() + " already exists");
        }

        locationRepository.save(location);

        return locationRepository.findAll();
    }

    @RequestMapping(path="/location", method=RequestMethod.PUT)
    public @ResponseBody Iterable<Location> updateLocation(@RequestBody Location location) throws Exception {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(!existing.isPresent()) {
            throw new Exception(location.getId() + " does not exist");
        }

        locationRepository.save(location);

        return locationRepository.findAll();
    }

    @RequestMapping(path="/location", method=RequestMethod.DELETE)
    public @ResponseBody Iterable<Location> deleteLocation(@RequestBody Location location) throws Exception {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(!existing.isPresent()) {
            throw new Exception(location.getId() + " does not exist");
        }

        locationRepository.delete(location);

        return locationRepository.findAll();
    }

}
