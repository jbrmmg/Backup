package com.jbr.middletier.backup.dto;

import lombok.Data;

@Data
public class PrintSizeDTO {
    private Integer id;
    private String name;
    private Double width;
    private Double height;
    private Boolean retro;
    private Boolean panoramic;
}
