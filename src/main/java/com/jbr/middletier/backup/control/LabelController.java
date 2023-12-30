package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.FileSystemObject;
import com.jbr.middletier.backup.data.FileSystemObjectId;
import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.dto.FileInfoExtra;
import com.jbr.middletier.backup.dto.FileLabelDTO;
import com.jbr.middletier.backup.dto.LabelDTO;
import com.jbr.middletier.backup.exception.InvalidFileIdException;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import com.jbr.middletier.backup.manager.LabelManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/jbr/int/backup")
public class LabelController {
    private final LabelManager labelManager;

    private final FileSystemObjectManager fileSystemObjectManager;

    @Autowired
    public LabelController(LabelManager labelManager, FileSystemObjectManager fileSystemObjectManager) {
        this.labelManager = labelManager;
        this.fileSystemObjectManager = fileSystemObjectManager;
    }

    @PostMapping(path="label")
    public FileInfoExtra addLabel(@RequestBody FileLabelDTO fileLabelDTO) throws InvalidFileIdException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(fileLabelDTO.getFileId(), FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(fileLabelDTO.getFileId());
        }

        for(Integer label : fileLabelDTO.getLabels()) {
            labelManager.addLabelToFile(file.get().getIdAndType(), label);
        }

        return fileSystemObjectManager.getFileExtra(fileLabelDTO.getFileId());
    }

    @DeleteMapping(path="label")
    public FileInfoExtra removeLabel(@RequestBody FileLabelDTO fileLabelDTO) throws InvalidFileIdException {
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(fileLabelDTO.getFileId(),FileSystemObjectType.FSO_FILE));

        if(file.isEmpty()) {
            throw new InvalidFileIdException(fileLabelDTO.getFileId());
        }

        for(Integer label : fileLabelDTO.getLabels()) {
            labelManager.removeLabelFromFile(file.get().getIdAndType(), label);
        }

        return fileSystemObjectManager.getFileExtra(fileLabelDTO.getFileId());
    }

    @GetMapping(path="labels")
    public List<LabelDTO> labels() {
        return labelManager.getLabels();
    }
}
