package com.jbr.middletier.backup.dto;

import javax.validation.constraints.NotNull;

@SuppressWarnings({"unused", "WeakerAccess"})
public class HardwareDTO {
    private String macAddress;
    private String reservedIP;
    private String ip;
    private String name;

    public HardwareDTO() {
        setMacAddress("");
        setReservedIP("N");
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(@NotNull String macAddress) {
        this.macAddress = macAddress;
    }

    public String getReservedIP() {
        return reservedIP;
    }

    public void setReservedIP(@NotNull String reservedIP) {
        this.reservedIP = reservedIP;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
