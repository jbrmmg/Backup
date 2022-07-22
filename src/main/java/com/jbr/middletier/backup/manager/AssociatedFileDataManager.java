package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.*;
import com.jbr.middletier.backup.dto.*;
import com.jbr.middletier.backup.exception.*;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.TypeMap;
import org.modelmapper.spi.Mapping;
import org.modelmapper.spi.MappingContext;
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
    private final PreImportSourceRepository preImportSourceRepository;
    private List<Classification> cachedClassifications;
    private ModelMapper modelMapper;

    @Autowired
    public AssociatedFileDataManager(SourceRepository sourceRepository, LocationRepository locationRepository, ClassificationRepository classificationRepository, SynchronizeRepository synchronizeRepository, ImportSourceRepository importSourceRepository, PreImportSourceRepository preImportSourceRepository) {
        this.sourceRepository = sourceRepository;
        this.locationRepository = locationRepository;
        this.classificationRepository = classificationRepository;
        this.synchronizeRepository = synchronizeRepository;
        this.importSourceRepository = importSourceRepository;
        this.preImportSourceRepository = preImportSourceRepository;
        this.cachedClassifications = null;

        this.modelMapper = new ModelMapper();

        PropertyMap<Source,SourceDTO> sourceMap = new PropertyMap<Source, SourceDTO>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        PropertyMap<ImportSource,ImportSourceDTO> importSourceMap = new PropertyMap<ImportSource, ImportSourceDTO>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
                map().setDestinationId(source.getDestination().getIdAndType().getId());
            }
        };

        PropertyMap<PreImportSource,PreImportSourceDTO> preImportSourceMap = new PropertyMap<PreImportSource, PreImportSourceDTO>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        this.modelMapper.addMappings(sourceMap);
        this.modelMapper.addMappings(importSourceMap);
        this.modelMapper.addMappings(preImportSourceMap);
    }

    // DTO to entity conversions.
    public ClassificationDTO convertToDTO(Classification classification) {
        return modelMapper.map(classification, ClassificationDTO.class);
    }

    public Classification convertToEntity(ClassificationDTO classification) {
        return modelMapper.map(classification, Classification.class);
    }

    public LocationDTO convertToDTO(Location location) {
        return modelMapper.map(location, LocationDTO.class);
    }

    public Location convertToEntity(LocationDTO location) {
        return modelMapper.map(location, Location.class);
    }

    public SourceDTO convertToDTO(Source source) {
        return modelMapper.map(source, SourceDTO.class);
    }

    public Source convertToEntity(SourceDTO source) {
        return modelMapper.map(source, Source.class);
    }

    public ImportSourceDTO convertToDTO(ImportSource source) {
        return modelMapper.map(source, ImportSourceDTO.class);
    }

    public ImportSource convertToEntity(ImportSourceDTO source) {
        return modelMapper.map(source, ImportSource.class);
    }

    public PreImportSourceDTO convertToDTO(PreImportSource source) {
        return modelMapper.map(source, PreImportSourceDTO.class);
    }

    public PreImportSource convertToEntity(PreImportSourceDTO source) {
        return modelMapper.map(source, PreImportSource.class);
    }

    public SynchronizeDTO convertToDTO(Synchronize synchronize) {
        return modelMapper.map(synchronize, SynchronizeDTO.class);
    }

    public Synchronize convertToEntity(SynchronizeDTO synchronize) {
        return modelMapper.map(synchronize, Synchronize.class);
    }

    /* --------------------------------------------------------------------------------------------------
     * CLASSIFICATION
     * -------------------------------------------------------------------------------------------------- */

    public List<Classification> findAllClassifications() {
        if(this.cachedClassifications == null) {
            this.cachedClassifications = new ArrayList<>();
            this.classificationRepository.findAllByOrderByOrderAsc().forEach(classification -> this.cachedClassifications.add(classification));
        }

        return this.cachedClassifications;
    }

    public Optional<Classification> classifyFile(FileInfo file) {
        for(Classification nextClassification : findAllClassifications()) {
            if(nextClassification.fileMatches(file)) {
                return Optional.of(nextClassification);
            }
        }

        return Optional.empty();
    }

    public Classification createClassification(Classification classification) throws ClassificationIdException {
        if(classification.getId() != null) {
            throw new ClassificationIdException();
        }

        this.cachedClassifications = null;
        return classificationRepository.save(classification);
    }

    public void updateClassification(Classification classification) throws InvalidClassificationIdException {
        Optional<Classification> existing = classificationRepository.findById(classification.getId());
        if(!existing.isPresent()) {
            throw new InvalidClassificationIdException(classification.getId());
        }

        this.cachedClassifications = null;
        classificationRepository.save(classification);
    }

    public void deleteClassification(Classification classification) throws InvalidClassificationIdException {
        Optional<Classification> existing = classificationRepository.findById(classification.getId());
        if(!existing.isPresent()) {
            throw new InvalidClassificationIdException(classification.getId());
        }

        this.cachedClassifications = null;
        classificationRepository.deleteById(classification.getId());
    }

    /* --------------------------------------------------------------------------------------------------
     * CLASSIFICATION
     * -------------------------------------------------------------------------------------------------- */

   public List<Location> findAllLocation() {
        List<Location> result = new ArrayList<>();

        this.locationRepository.findAllByOrderByIdAsc().forEach(result::add);

        return result;
    }

    public Optional<Location> findImportLocation() {
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

    @SuppressWarnings("UnusedReturnValue")
    public Location createLocation(Location location) throws LocationAlreadyExistsException {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(existing.isPresent()) {
            throw new LocationAlreadyExistsException(existing.get().getId());
        }

        return locationRepository.save(location);
    }

    public void updateLocation(Location location) throws InvalidLocationIdException {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(!existing.isPresent()) {
            throw new InvalidLocationIdException(location.getId());
        }

        locationRepository.save(location);
    }

    public void deleteLocation(Location location) throws InvalidLocationIdException {
        Optional<Location> existing = locationRepository.findById(location.getId());
        if(!existing.isPresent()) {
            throw new InvalidLocationIdException(location.getId());
        }

        locationRepository.deleteById(location.getId());
    }

    /* --------------------------------------------------------------------------------------------------
     * SOURCE
     * -------------------------------------------------------------------------------------------------- */

    public List<Source> findAllSource() {
        List<Source> result = new ArrayList<>();

        this.sourceRepository.findAllByOrderByIdAsc().forEach(result::add);

        return result;
    }

    public Source findSourceById(Integer id) throws InvalidSourceIdException {
        Optional<Source> existing = sourceRepository.findById(id);
        if(!existing.isPresent()) {
            throw new InvalidSourceIdException(id);
        }

        return existing.get();
    }

    public Optional<Source> findSourceIfExists(Integer id) {
        return sourceRepository.findById(id);
    }

    public Source createSource(Source source) throws SourceAlreadyExistsException {
        if(source.getIdAndType().getId() != null) {
            throw new SourceAlreadyExistsException(source.getIdAndType().getId());
        }

        return sourceRepository.save(source);
    }

    public void updateSource(Source source) throws InvalidSourceIdException {
        // Check it exists
        findSourceById(source.getIdAndType().getId());

        sourceRepository.save(source);
    }

    public void updateSourceStatus(Source source, SourceStatusType status) {
        try {
            source.setStatus(status);
            sourceRepository.save(source);
        } catch(Exception ex) {
            LOG.warn("Failed to set source status. (ignored the error)",ex);
        }
    }

    public void deleteSource(Source source) throws InvalidSourceIdException {
        // Check it exists
        findSourceById(source.getIdAndType().getId());

        sourceRepository.deleteById(source.getIdAndType().getId());
    }

    public void deleteAllSource() {
        sourceRepository.deleteAll();
    }

    /* --------------------------------------------------------------------------------------------------
     * IMPORT SOURCE
     * -------------------------------------------------------------------------------------------------- */

    public List<ImportSource> findAllImportSource() {
        List<ImportSource> result = new ArrayList<>();

        this.importSourceRepository.findAllByOrderByIdAsc().forEach(result::add);

        return result;
    }

    public Optional<ImportSource> findImportSourceIfExists(Integer id) {
        return importSourceRepository.findById(id);
    }

    public ImportSource createImportSource(ImportSource source) throws SourceAlreadyExistsException {
        if(source.getIdAndType().getId() != null) {
            throw new SourceAlreadyExistsException(source.getIdAndType().getId());
        }

        return importSourceRepository.save(source);
    }

    public void updateImportSource(ImportSource source) throws InvalidSourceIdException {
        // Check it exists
        findSourceById(source.getIdAndType().getId());

        importSourceRepository.save(source);
    }

    public void deleteImportSource(ImportSource source) throws InvalidSourceIdException {
        // Check it exists
        findSourceById(source.getIdAndType().getId());

        importSourceRepository.deleteById(source.getIdAndType().getId());
    }

    public void deleteAllImportSource() {
        importSourceRepository.deleteAll();
    }

    /* --------------------------------------------------------------------------------------------------
     * PRE-IMPORT SOURCE
     * -------------------------------------------------------------------------------------------------- */

    public List<PreImportSource> findAllPreImportSource() {
        List<PreImportSource> result = new ArrayList<>();

        this.preImportSourceRepository.findAllByOrderByIdAsc().forEach(result::add);

        return result;
    }

    public Optional<PreImportSource> findPreImportSourceIfExists(Integer id) {
        return preImportSourceRepository.findById(id);
    }

    public PreImportSource createPreImportSource(PreImportSource source) throws SourceAlreadyExistsException {
        if(source.getIdAndType().getId() != null) {
            throw new SourceAlreadyExistsException(source.getIdAndType().getId());
        }

        return preImportSourceRepository.save(source);
    }

    public void updatePreImportSource(PreImportSource source) throws InvalidSourceIdException {
        // Check it exists
        findSourceById(source.getIdAndType().getId());

        preImportSourceRepository.save(source);
    }

    public void deletePreImportSource(PreImportSource source) throws InvalidSourceIdException {
        // Check it exists
        findSourceById(source.getIdAndType().getId());

        preImportSourceRepository.deleteById(source.getIdAndType().getId());
    }

    public void deleteAllPreImportSource() {
        preImportSourceRepository.deleteAll();
    }

    /* --------------------------------------------------------------------------------------------------
     * SYNCHRONIZE
     * -------------------------------------------------------------------------------------------------- */

    public List<Synchronize> findAllSynchronize() {
        List<Synchronize> result = new ArrayList<>();

        this.synchronizeRepository.findAllByOrderByIdAsc().forEach(result::add);

        return result;
    }

    public Synchronize createSynchronize(Synchronize synchronize) throws SynchronizeAlreadyExistsException, InvalidSourceIdException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(existing.isPresent()) {
            throw new SynchronizeAlreadyExistsException(synchronize.getId());
        }

        return synchronizeRepository.save(synchronize);
    }

    public void updateSynchronize(Synchronize synchronize) throws InvalidSynchronizeIdException, InvalidSourceIdException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new InvalidSynchronizeIdException(synchronize.getId());
        }

        synchronizeRepository.save(synchronize);
    }

    public void deleteSynchronize(Synchronize synchronize) throws InvalidSynchronizeIdException {
        Optional<Synchronize> existing = synchronizeRepository.findById(synchronize.getId());
        if(!existing.isPresent()) {
            throw new InvalidSynchronizeIdException(synchronize.getId());
        }

        synchronizeRepository.deleteById(synchronize.getId());
    }

    public void deleteAllSynchronize() {
        synchronizeRepository.deleteAll();
    }
}
