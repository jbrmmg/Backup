package com.jbr.middletier.backup.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BackupJobRunDTO {
    private String backupId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
    private String message;
}
