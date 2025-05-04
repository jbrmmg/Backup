package com.jbr.middletier.backup.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileExpiryDTO {
    private Integer id;
    private LocalDateTime expiry;
}
