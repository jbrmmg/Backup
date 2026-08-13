package com.jbr.middletier.backup.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileDateUpdateDTO {
    private Integer id;
    private LocalDateTime date;
}
