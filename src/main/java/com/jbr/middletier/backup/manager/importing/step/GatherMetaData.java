package com.jbr.middletier.backup.manager.importing.step;

import com.jbr.middletier.backup.data.ImportFile;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.ImportFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystem;
import com.jbr.middletier.backup.manager.FileSystemImageData;
import com.jbr.middletier.backup.manager.importing.FileProcessingStepType;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
public class GatherMetaData extends ImportStep {
    private static final Logger LOG = LoggerFactory.getLogger(GatherMetaData.class);

    private final FileSystem fileSystem;

    protected GatherMetaData(ImportFileRepository importFileRepository,
                             ImportSourceManager importSourceManager,
                             AssociatedFileDataManager associatedFileDataManager,
                             FileSystem fileSystem) {
        super(importFileRepository, associatedFileDataManager, importSourceManager);
        this.fileSystem = fileSystem;
    }

    @Override
    protected boolean transferData(PreImportFileDTO file, ImportFile dbRecord) {
        // Is the image / video flag already stored?
        if(dbRecord.getImage() == null || dbRecord.getVideo() == null) {
            // Transfer the data.
            dbRecord.setDuration(file.getDuration());
            if(file.getImageSize() != null) {
                dbRecord.setImageHeight(file.getImageSize().height());
                dbRecord.setImageWidth(file.getImageSize().width());
            } else {
                dbRecord.setImageHeight(null);
                dbRecord.setImageWidth(null);
            }
            if(file.getLocation() != null) {
                dbRecord.setLatitude(file.getLocation().getLatitude());
                dbRecord.setLongitude(file.getLocation().getLongitude());
            } else {
                dbRecord.setLatitude(null);
                dbRecord.setLongitude(null);
            }
            dbRecord.setVideo(file.isVideo());
            dbRecord.setImage(file.isImage());

            if(dbRecord.getImportDate() == null) {
                dbRecord.setImportDate(file.getImportDate());
            }
            LOG.info("Update the meta data on the database for {}", file.getFilename());
            return true;
        }

        LOG.info("No need to update the meta data {}", file.getFilename());
        return false;
    }

    @Override
    public FileProcessingStepType getStepType() {
        return FileProcessingStepType.FPS_GATHER_META_DATA;
    }

    @Override
    public TrafficLightType performStep(PreImportFileDTO file) {
        // If the file data has image size or duration then don't query the metadata again.
        if(file.getImageSize() != null || file.getDuration() != null) {
            LOG.info("{} meta data is already  gathered", file.getFilename());
            return TrafficLightType.TL_GREEN;
        }

        // Read the metadata of the import file.
        File preImportFile = getPreImportFilename(file);
        Optional<FileSystemImageData> imageData = fileSystem.readImageMetaData(preImportFile);

        if(imageData.isPresent()) {
            LOG.info("{} contains meta data", file.getFilename());
            // Update the file data.
            file.setVideo(imageData.get().getMimeType().toLowerCase().startsWith("video"));
            file.setImage(imageData.get().getMimeType().toLowerCase().startsWith("image"));
            file.setImageSize(imageData.get().getImageSize());
            file.setDuration(imageData.get().getDuration());
            file.setLocation(imageData.get().getLatLong());
            file.setImportDate(imageData.get().getDateTime());
            LOG.info("{} is video {} or image {}", file.getFilename(), file.isVideo(), file.isImage());
        } else {
            LOG.info("{} does not contain meta data", file.getFilename());
            // Flag that this is not an image or video.
            file.setVideo(false);
            file.setImage(false);
        }
        saveData(file);

        return TrafficLightType.TL_GREEN;
    }
}
