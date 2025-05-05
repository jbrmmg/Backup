package com.jbr.middletier.backup.dto;

import com.jbr.middletier.backup.data.ClassificationActionType;
import lombok.Data;

@Data
public class ClassificationDTO {
    private Integer id;
    private Integer order;
    private String regex;
    private ClassificationActionType action;
    private String icon;
    private Boolean useMD5;
    private Boolean isImage;
    private Boolean isVideo;
    private Boolean checkMetaData;
}
