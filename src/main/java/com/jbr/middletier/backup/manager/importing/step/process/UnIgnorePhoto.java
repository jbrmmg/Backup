package com.jbr.middletier.backup.manager.importing.step.process;

import com.jbr.middletier.backup.data.IgnoreFile;
import com.jbr.middletier.backup.data.ImportFileStatusType;
import com.jbr.middletier.backup.data.TrafficLightType;
import com.jbr.middletier.backup.dataaccess.IgnoreFileRepository;
import com.jbr.middletier.backup.dto.PreImportFileDTO;
import com.jbr.middletier.backup.manager.importing.ImportSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class UnIgnorePhoto extends ProcessBase {
    private static final Logger LOG = LoggerFactory.getLogger(UnIgnorePhoto.class);

    private final IgnoreFileRepository ignoreFileRepository;

    @Autowired
    protected UnIgnorePhoto(ImportSourceManager importSourceManager, IgnoreFileRepository ignoreFileRepository) {
        super(ImportFileStatusType.IFS_UN_IGNORE, importSourceManager);
        this.ignoreFileRepository = ignoreFileRepository;
    }

    @Override
    public TrafficLightType process(PreImportFileDTO file) throws ImportProcessException {
        // Is this file in the ignore table?
        List<IgnoreFile> ignoredFileList = ignoreFileRepository.findByMd5(file.getImportMd5());

        // If found, remove it.
        for(IgnoreFile next : ignoredFileList) {
            LOG.info("Remove from ignore {}.", file.getFilename());
            ignoreFileRepository.delete(next);
        }

        return TrafficLightType.TL_GREEN;
    }
}
