package com.jbr.middletier.backup.dto;

import java.time.LocalDateTime;

public class FileExpiryDTO {
    private Integer id;
    private LocalDateTime expiry;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getExpiry() {
        return expiry;
    }

    public void setExpiry(LocalDateTime expiry) {
        this.expiry = expiry;
    }
}
