package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AssociatedFileDataManager {
    private static final Logger LOG = LoggerFactory.getLogger(AssociatedFileDataManager.class);

    private final SourceRepository sourceRepository;
    private final LocationRepository locationRepository;
    private final ClassificationRepository classificationRepository;
    private final SynchronizeRepository synchronizeRepository;
    private final ImportSourceRepository importSourceRepository;
    private List<Classification> cachedClassifications;

    @Autowired
    public AssociatedFileDataManager(SourceRepository sourceRepository, LocationRepository locationRepository, ClassificationRepository classificationRepository, SynchronizeRepository synchronizeRepository, ImportSourceRepository importSourceRepository) {
        this.sourceRepository = sourceRepository;
        this.locationRepository = locationRepository;
        this.classificationRepository = classificationRepository;
        this.synchronizeRepository = synchronizeRepository;
        this.importSourceRepository = importSourceRepository;
        this.cachedClassifications = null;
    }

    // Classification - CRUD actions.
    public List<ClassificationDTO> externalFindAllClassification() {
        List<ClassificationDTO> result = new ArrayList<>();

        this.classificationRepository.findAllByOrderByOrderAsc().forEach(classification -> result.add(new ClassificationDTO(classification)));

        return result;
    }

    public List<Classification> internalFindAllClassification() {
        if(this.cachedClassifications == null) {
            this.cachedClassifications = new ArrayList<>();
            this.classificationRepository.findAllByOrderByOrderAsc().forEach(classification -> this.cachedClassifications.add(classification));
        }

        return this.cachedClassifications;
    }

    public Classification classifyFile(FileInfo file) {
        for(Classification nextClassification : internalFindAllClassification()) {
            if(nextClassification.fileMatches(file)) {
                return nextClassification;
            }
        }

        return null;
    }

    public Classification createClassification(ClassificationDTO newClassification) throws ClassificationIdException {
        if(newClassification.getId() != null) {
            throw new ClassificationIdException();
        }

        this.cachedClassifications = null;
        return classificationRepository.save(new Classification(newClassification));
    }

    public void updateClassification(ClassificationDTO classification) throws InvalidClassificationIdException {
        Optional<Classification> existing = classificationRepository.findById(classification.getId());
        if(!existing.isPresent()) {
            throw new InvalidClassificationIdException(classification.getId());
        }

        existing.get().update(classification);

        this.cachedClassifications = null;
        classificationRepository.save(existing.get());
    }

    public void deleteClassification(ClassificationDTO classification) throws InvalidClassificationIdException {
        Optional<Classification> existing = classificationRepository.findById(classification.getId());
        if(!existing.isPresent()) {
            throw new InvalidClassificationIdException(classification.getId());
        }

        this.cachedClassifications = null;
        classificationRepository.deleteById(classification.getId());
    }

    // Location - CRUD actions
    public List<LocationDTO> externalFindAllLocation() {
        List<LocationDTO> result = new ArrayList<>();

        this.locationRepository.findAllByOrderByIdAsc().forEach(location -> result.add(new LocationDTO(location)));

        return result;
    }

    public Optional<Location> internalFindImportLocationIfExists() {
        Optional<Location> result = Optional.empty();
        for(Location nextLocation: locationRepository.findAll()) {
            if(nextLocation.getName().equalsIgnoreCase("import")) {
                result = Optional.of(nextLocation);
            }
        }

        return result;
    }

    public Optional<Location> findLocationById(int id) {
        return locationRepository.findById(id);
    }

    public Location createLocation(LocationDTO newLocation) throws LocationAlreadyExistsException {
        Optional<Location> existing = locationRepository.findById(newLocation.getId());
        if(existing.isPresent()) {
            throw new LocationAlreadyExistsException(existing.get().getId());
        }

        return locationRepository.save(new Location(newLocation));
    }

    public void updateLocation(LocationDTO location) throws InvalidLocationIdException {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(!existing.isPresent()) {
            throw new InvalidLocationIdException(location.getId());
        }

        existing.get().update(location);
        locationRepository.save(existing.get());
    }

    public void deleteLocation(LocationDTO location) throws InvalidLocationIdException {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(!existing.isPresent()) {
            throw new InvalidLocationIdException(location.getId());
        }

        locationRepository.deleteById(location.getId());
    }

    // Source - CRUD actions
    public List<SourceDTO> externalFindAllSource() {
        List<SourceDTO> result = new ArrayList<>();

        this.sourceRepository.findAllByOrderByIdAsc().forEach(source -> result.add(new SourceDTO(source)));

        return result;
    }

    public List<ImportSourceDTO> externalFindAllImportSource() {
        List<ImportSourceDTO> result = new ArrayList<>();

        this.internalFindAllImportSource().forEach(importSource -> result.add(new ImportSourceDTO(importSource)));

        return result;
    }

    public Iterable<ImportSource> internalFindAllImportSource() {
        return this.importSourceRepository.findAllByOrderByIdAsc();
    }

    public Iterable<Source> internalFindAllSource() {
        return this.sourceRepository.findAll();
    }

    public Source internalFindSourceById(Integer id) throws InvalidSourceIdException {
        Optional<Source> existing = sourceRepository.findById(id);
        if(!existing.isPresent()) {
            throw new InvalidSourceIdException(id);
        }

        return existing.get();
    }

    public Optional<Source> internalFindSourceByIdIfExists(Integer id) {
        return sourceRepository.findById(id);
    }

    public Optional<ImportSource> internalFindImportSourceByIdIfExists(Integer id) {
        return importSourceRepository.findById(id);
    }

    public Source createSource(SourceDTO newSource) throws SourceAlreadyExistsException {
        if(newSource.getId() != null) {
            throw new SourceAlreadyExistsException(newSource.getId());
        }

        return sourceRepository.save(new Source(newSource));
    }

    public ImportSource createImportSource(String path, Source destination, Location location) {
        ImportSource importSource = new ImportSource(path);
        importSource.setDestination(destination);
        importSource.setLocation(location);
        importSource.setStatus(SourceStatusType.SST_OK);

        sourceRepository.save(importSource);

        return importSource;
    }

    public void updateSource(SourceDTO source) throws InvalidSourceIdException {
        Source existing = internalFindSourceById(source.getId());
        existing.update(source);
        sourceRepository.save(existing);
    }

    public void updateSourceStatus(Source source, SourceStatusType status) {
        try {
            source.setStatus(status);
            sourceRepository.save(source);
        } catch(Exception ex) {
            LOG.warn("Failed to set source status. (ignored the error)",ex);
        }
    }

    public void deleteSource(SourceDTO source) throws InvalidSourceIdException {
        sourceRepository.deleteById(internalFindSourceById(source.getId()).getIdAndType().getId());
    }

    public void internalDeleteSource(Source source) {
        sourceRepository.delete(source);
    }

    public void deleteImportSource(ImportSourceDTO importSource) {
        importSourceRepository.deleteById(importSource.getId());
    }

    public void deleteAllSource() {
        sourceRepository.deleteAll();
    }

    public void deleteAllImportSource() {
        importSourceRepository.deleteAll();
    }

    // Synchronize - CRUD actions
    public List<SynchronizeDTO> externalFindAllSynchronize() {
        List<SynchronizeDTO> result = new ArrayList<>();

        this.synchronizeRepository.findAllByOrderByIdAsc().forEach(synchronize -> result.add(new SynchronizeDTO(synchronize)));

        return result;
    }

    public Iterable<Synchronize> internalFindAllSynchronize() {
        return this.synchronizeRepository.findAll();
    }

    public Synchronize createSynchronize(SynchronizeDTO newSynchronize) throws SynchronizeAlreadyExistsException, InvalidSourceIdException {
        Optional<Synchronize> existing = synchronizeRepository.findById(newSynchronize.getId());
        if(existing.isPresent()) {
            throw new SynchronizeAlreadyExistsException(newSynchronize.getId());
        }

        Source source = internalFindSourceById(newSynchronize.getSource().getId());
        Source destination = internalFindSourceById(newSynchronize.getDestination().getId());

        Synchronize newSync = new Synchronize();
        newSync.setId(newSynchronize.getId());
        newSync.setDestination(destination);
        newSync.setSource(source);

        return synchronizeRepository.save(newSync);
    }

    public void updateSynchronize(SynchronizeDTO synchronize) throws InvalidSynchronizeIdException, InvalidSourceIdException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new InvalidSynchronizeIdException(synchronize.getId());
        }

        Source source = internalFindSourceById(synchronize.getSource().getId());
        Source destination = internalFindSourceById(synchronize.getDestination().getId());

        existing.get().setDestination(destination);
        existing.get().setSource(source);

        synchronizeRepository.save(existing.get());
    }

    public void deleteSynchronize(SynchronizeDTO synchronize) throws InvalidSynchronizeIdException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new InvalidSynchronizeIdException(synchronize.getId());
        }

        synchronizeRepository.deleteById(existing.get().getId());
    }

    public void internalDeleteSynchronize(Synchronize synchronize) {
        synchronizeRepository.delete(synchronize);
    }

    public void deleteAllSynchronize() {
        synchronizeRepository.deleteAll();
    }
}
