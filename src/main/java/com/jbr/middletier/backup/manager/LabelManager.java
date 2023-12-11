package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.FileLabelRepository;
import com.jbr.middletier.backup.dataaccess.LabelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LabelManager {
    private static final Logger LOG = LoggerFactory.getLogger(LabelManager.class);

    private final LabelRepository labelRepository;
    private final FileLabelRepository fileLabelRepository;
    private final Map<Integer,String> labels;
    private boolean labelsLoaded;

    @Autowired
    public LabelManager(LabelRepository labelRepository, FileLabelRepository fileLabelRepository) {
        this.labelRepository = labelRepository;
        this.fileLabelRepository = fileLabelRepository;
        this.labels = new HashMap<>();
        this.labelsLoaded = false;
    }

    private void loadLabels() {
        if(!labelsLoaded) {
            for(Label next : this.labelRepository.findAll()) {
                this.labels.put(next.getId(), next.getName());
            }
            this.labelsLoaded = true;
        }
    }

    public Collection<String> getLabels() {
        LOG.trace("Get labels");
        loadLabels();

        return this.labels.values();
    }

    public List<String> getLabelsForFile(FileSystemObjectId id) {
        List<String> result = new ArrayList<>();

        // Labels only apply to files.
        if(id.getType() != FileSystemObjectType.FSO_FILE) {
            return result;
        }

        loadLabels();
        for(FileLabel next : fileLabelRepository.findByIdFileId(id.getId())) {
            if(this.labels.containsKey(next.getId().getLabelId())) {
                result.add(this.labels.get(next.getId().getLabelId()));
            }
        }

        return result;
    }

    public void addLabelToFile(FileSystemObjectId id, Integer labelId) {
        // Labels only apply to files.
        if(id.getType() != FileSystemObjectType.FSO_FILE) {
            return;
        }

        loadLabels();

        if(this.labels.containsKey(labelId)) {
            // Check it's not already added.
            for(FileLabel next : fileLabelRepository.findByIdFileId(id.getId())) {
                if(next.getId().getLabelId().equals(labelId)) {
                    return;
                }
            }

            // Save the new label id.
            FileLabel newLabel = new FileLabel();
            FileLabelId newLabelId = new FileLabelId();
            newLabelId.setFileId(id.getId());
            newLabelId.setLabelId(labelId);
            newLabel.setId(newLabelId);

            fileLabelRepository.save(newLabel);
        }
    }

    public void removeLabelFromFile(FileSystemObjectId id, Integer labelId) {
        // Labels only apply to files.
        if(id.getType() != FileSystemObjectType.FSO_FILE) {
            return;
        }

        loadLabels();

        if(this.labels.containsKey(labelId)) {
            // Check that this label is present.
            for(FileLabel next : fileLabelRepository.findByIdFileId(id.getId())) {
                if(next.getId().getLabelId().equals(labelId)) {
                    // Remove this label.
                    fileLabelRepository.save(next);
                    return;
                }
            }
        }
    }
}
