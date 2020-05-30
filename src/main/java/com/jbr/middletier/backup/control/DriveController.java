package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.manager.DriveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/jbr/int/backup")
public class DriveController {
    final static private Logger LOG = LoggerFactory.getLogger(DriveController.class);

    final private DriveManager driveManager;
    final private LocationRepository locationRepository;
    final private ClassificationRepository classificationRepository;
    final private SynchronizeRepository synchronizeRepository;
    final private SourceRepository sourceRepository;
    final private FileRepository fileRepository;

    @Autowired
    public DriveController(DriveManager driverManager,
                           LocationRepository locationRepository,
                           ClassificationRepository classificationRepository,
                           SynchronizeRepository synchronizeRepository,
                           SourceRepository sourceRepository,
                           FileRepository fileRepository) {
        this.driveManager = driverManager;
        this.locationRepository = locationRepository;
        this.classificationRepository = classificationRepository;
        this.synchronizeRepository = synchronizeRepository;
        this.sourceRepository = sourceRepository;
        this.fileRepository = fileRepository;
    }

    @RequestMapping(path="/location", method=RequestMethod.GET)
    public @ResponseBody Iterable<Location> getLocation() {
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

    @RequestMapping(path="/classification", method=RequestMethod.GET)
    public @ResponseBody Iterable<Classification> getClassification() { return classificationRepository.findAll(); }

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

    @RequestMapping(path="/synchronize", method=RequestMethod.GET)
    public @ResponseBody Iterable<Synchronize> getSynchronize() { return synchronizeRepository.findAll(); }

    @RequestMapping(path="/synchronize", method=RequestMethod.POST)
    public @ResponseBody Iterable<Synchronize> createSynchronize(@RequestBody Synchronize synchronize) throws Exception {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(existing.isPresent()) {
            throw new Exception(existing.get().getId() + " already exists");
        }

        synchronizeRepository.save(synchronize);

        return synchronizeRepository.findAll();
    }

    @RequestMapping(path="/synchronize", method=RequestMethod.PUT)
    public @ResponseBody Iterable<Synchronize> updateSynchronize(@RequestBody Synchronize synchronize) throws Exception {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new Exception(synchronize.getId() + " does not exist");
        }

        synchronizeRepository.save(synchronize);

        return synchronizeRepository.findAll();
    }

    @RequestMapping(path="/synchronize", method=RequestMethod.DELETE)
    public @ResponseBody Iterable<Synchronize> deleteSynchronize(@RequestBody Synchronize synchronize) throws Exception {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new Exception(synchronize.getId() + " does not exist");
        }

        synchronizeRepository.delete(synchronize);

        return synchronizeRepository.findAll();
    }

    @RequestMapping(path="/source", method=RequestMethod.GET)
    public @ResponseBody Iterable<Source> getSource() { return sourceRepository.findAll(); }

    @RequestMapping(path="/source", method=RequestMethod.POST)
    public @ResponseBody Iterable<Source> createSource(@RequestBody Source source) throws Exception {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(existing.isPresent()) {
            throw new Exception(existing.get().getId() + " already exists");
        }

        sourceRepository.save(source);

        return sourceRepository.findAll();
    }

    @RequestMapping(path="/source", method=RequestMethod.PUT)
    public @ResponseBody Iterable<Source> updateSource(@RequestBody Source source) throws Exception {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(!existing.isPresent()) {
            throw new Exception(source.getId() + " does not exist");
        }

        sourceRepository.save(source);

        return sourceRepository.findAll();
    }

    @RequestMapping(path="/source", method=RequestMethod.DELETE)
    public @ResponseBody Iterable<Source> deleteSource(@RequestBody Source source) throws Exception {
        Optional<Source> existing = sourceRepository.findById(source.getId());
        if(!existing.isPresent()) {
            throw new Exception(source.getId() + " does not exist");
        }

        sourceRepository.delete(source);

        return sourceRepository.findAll();
    }

    @RequestMapping(path="/files", method=RequestMethod.GET)
    public @ResponseBody Iterable<FileInfo> getFiles() { return fileRepository.findAll(); }

    @RequestMapping(path="/gather", method= RequestMethod.POST)
    public @ResponseBody OkStatus gather(@RequestBody String temp) {
        LOG.info("Process drive - " + temp);

        try {
            driveManager.gather();
        } catch (IOException e) {
            OkStatus status = new OkStatus();
            status.setStatus("Failed - " + e.toString());
        }

        return OkStatus.getOkStatus();
    }

    @RequestMapping(path="/sync", method= RequestMethod.POST)
    public @ResponseBody OkStatus synchronize(@RequestBody String temp) {
        LOG.info("Syncronize drives - " + temp);

        driveManager.synchronize();

        return OkStatus.getOkStatus();
    }
}
