package com.jbr.middletier.backup.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SearchRequestDTO {
    private String filename;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Long sizeMin;
    private Long sizeMax;
    private LocalDateTime expiryFrom;
    private LocalDateTime expiryTo;
    private List<String> labels;
    private SearchLocationDTO location;
    private int page = 0;
    private int pageSize = 20;
}
