package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.FileSystemObjectType;
import com.jbr.middletier.backup.data.MD5;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Optional;

@Data
public class ImportFileBaseDTO {
    private static final Logger LOG = LoggerFactory.getLogger(ImportFileBaseDTO.class);

    private volatile String filename;
    private volatile LocalDateTime date;
    private volatile Long size;
    @Getter
    private volatile String md5;
    private volatile FileSystemObjectType type;

    public ImportFileBaseDTO() {
        LOG.trace("Create new Import File Base DTO.");
    }

    public void setMd5(MD5 md5) { this.md5 = md5 != null ? md5.toString() : null; }

    public Optional<MD5> getMd5Optional() { return this.md5 == null || this.md5.isEmpty() ? Optional.empty() : Optional.of(new MD5(this.md5)); }
}
