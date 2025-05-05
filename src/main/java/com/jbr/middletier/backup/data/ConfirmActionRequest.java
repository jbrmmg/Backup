package com.jbr.middletier.backup.data;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConfirmActionRequest {
    private int id;
    private String parameter;
    private Boolean confirm;
}
