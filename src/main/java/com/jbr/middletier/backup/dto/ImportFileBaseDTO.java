package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileSystemObjectType;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

@Data
public class ImportFileBaseDTO {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFileBaseDTO.class);

    private volatile String filename;
    private volatile LocalDateTime date;
    private volatile Long size;
    private volatile String md5;
    private volatile FileSystemObjectType type;

    public ImportFileBaseDTO() {
        LOG.trace("Create new Import File Base DTO.");
    }
}
