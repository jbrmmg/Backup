package com.jbr.middletier.backup.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponseDTO {
    private int page;
    private int pageSize;
    private long totalCount;
    private List<SearchResultDTO> results;
}
