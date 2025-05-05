package com.jbr.middletier.backup.dto;

import lombok.Data;

@Data
public class HardwareDTO {
    private String macAddress;
    private String reservedIP;
    private String ip;
    private String name;

    public HardwareDTO() {
        setMacAddress("");
        setReservedIP("N");
    }
}
