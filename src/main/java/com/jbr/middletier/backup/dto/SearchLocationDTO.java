package com.jbr.middletier.backup.dto;

import lombok.Data;

@Data
public class SearchLocationDTO {
    private Double south;
    private Double west;
    private Double north;
    private Double east;
}
